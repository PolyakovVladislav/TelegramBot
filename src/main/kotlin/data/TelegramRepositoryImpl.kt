package data

import LOGGER_LEVEL
import data.dataSource.TelegramDataSource
import data.dataSource.remote.TelegramRemoteDataSource
import data.model.ScrapUsersResult
import data.model.TelegramAuthenticationResult
import domain.exception.TelegramException
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository
import it.tdlight.jni.TdApi.Chat
import it.tdlight.jni.TdApi.ChatTypeBasicGroup
import it.tdlight.jni.TdApi.ChatTypeSupergroup
import it.tdlight.jni.TdApi.MessageSenderUser
import kotlinx.coroutines.delay
import utils.Logger
import kotlin.math.floor

class TelegramRepositoryImpl(
    private val telegramDataSource: TelegramDataSource,
    private val configs: ConfigurationsRepository
) : TelegramRepository {

    private val logger = Logger(this.javaClass.simpleName, configs, LOGGER_LEVEL)

    private suspend fun <T> executeSafety(action: suspend () -> T): Result<T> {
        return try {
            Result.success(action())
        } catch (e: TelegramException) {
            logger.e(e)
            Result.failure(e)
        }
    }

    // apiId = 17020341; apiHash = c85117a0bebf5e59b0404a7127bd1447
    override suspend fun login(phoneNumber: Long, apiId: Int, apiHash: String): Result<TelegramAuthenticationResult> {
        return executeSafety {
            logger("login:", logLevel = LOGGER_LEVEL)
            telegramDataSource.login(phoneNumber, apiId, apiHash)
        }
    }

    override suspend fun confirmLogin(authCode: String): Result<Unit> {
        return executeSafety {
            logger("confirmLogin:", logLevel = LOGGER_LEVEL)
            telegramDataSource.confirmAuthorizationWithCode(authCode)
        }
    }

    override suspend fun scrapGroupForUsersIds(groupLink: String): Result<ScrapUsersResult> {
        return executeSafety {
            logger("scrapGroupForUsersIds:", logLevel = LOGGER_LEVEL)
            val chat = telegramDataSource.searchPublicChat(groupLink)
                when (chat.type) {
                    is ChatTypeSupergroup -> scarpUserIdsFromSuperGroup(chat)

                    is ChatTypeBasicGroup -> scrapUserIdsFromBasicGroup(chat)

                    else -> {
                        throw TelegramException(
                            TelegramRemoteDataSource.UNSUPPORTED_GROUP_TYPE,
                            "Unsupported group type: ${chat.type}"
                        )
                    }
                }
            }
//            val searchResult = telegramDataSource.searchPublicChats(groupLink)
//            if (searchResult.totalCount < 1) {
//                throw GroupIsNotFoundException("Group with name $groupLink not found")
//            } else {
//                searchResult.chatIds.forEach { chatId ->
//                    val chat = telegramDataSource.getChat(chatId)
//                    if (getChatLink(chat) == groupLink) {
//                        when (chat.type) {
//                            is ChatTypeSupergroup -> scarpUserIdsFromSuperGroup(chat)
//
//                            is ChatTypeBasicGroup -> scrapUserIdsFromBasicGroup(chat)
//
//                            else -> {
//                                throw TelegramException(
//                                    TelegramRemoteDataSource.UNSUPPORTED_GROUP_TYPE,
//                                    "Unsupported group type: ${chat.type}"
//                                )
//                            }
//                        }
//                    }
//                }
//                throw GroupIsNotFoundException("Group with link: $groupLink not found")
//            }
    }

    private suspend fun getChatLink(chat: Chat): String {
        when (chat.type) {
            is ChatTypeSupergroup -> {
                val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
                val info = telegramDataSource.getSuperGroupFullInfo(supergroupId)
                return info.inviteLink.name
            }
            is ChatTypeBasicGroup -> {
                val info = telegramDataSource.getBasicGroupFullInfo(chat.id)
                return info.inviteLink.name
            }
            else -> {
                throw TelegramException(
                    TelegramRemoteDataSource.UNSUPPORTED_GROUP_TYPE,
                    "Unsupported group type: ${chat.type}"
                )
            }
        }
    }

    private suspend fun scarpUserIdsFromSuperGroup(chat: Chat): ScrapUsersResult {
        val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
        val superGroupInfo = telegramDataSource.getSuperGroupFullInfo(supergroupId)
        val membersCount = superGroupInfo.memberCount
        if (superGroupInfo.canGetMembers) {
            val iMax = if (membersCount <= 200) {
                0
            } else {
                floor(membersCount / 200f).toInt()
            }
            val list = mutableListOf<Long>()
            for (i in 0..iMax) {
                delay(1000)
                val members = try {
                    telegramDataSource.getSuperGroupMembers(supergroupId, i * 200, 200)
                } catch (e: TelegramException) {
                    if (list.isEmpty()) {
                        throw e
                    } else {
                        return ScrapUsersResult(
                            list,
                            e
                        )
                    }
                }
                list.addAll(
                    (members.members.map { (it.memberId as MessageSenderUser).userId })
                )
            }
            return ScrapUsersResult(
                list,
                null
            )
        } else {
            throw TelegramException(
                TelegramRemoteDataSource.GROUP_CANT_BE_SCRAPED,
                "Group ${superGroupInfo.inviteLink} can't be scrapped by group restrictions"
            )
        }
    }

    private suspend fun scrapUserIdsFromBasicGroup(chat: Chat): ScrapUsersResult {
        val basicGroupInfo = telegramDataSource.getBasicGroupFullInfo(chat.id)
        val ids = basicGroupInfo.members.map { member ->
            (member.memberId as MessageSenderUser).userId
        }
        return ScrapUsersResult(ids, null)
    }

    override suspend fun getChatId(groupLink: String): Result<Long> {
        return executeSafety {
            logger("getChatId:", logLevel = LOGGER_LEVEL)
            val searchResult = telegramDataSource.searchPublicChat(groupLink)
            return@executeSafety searchResult.id
//            if (searchResult.totalCount < 1) {
//                throw GroupIsNotFoundException("Group with name $groupName not found")
//            } else {
//                searchResult.chatIds.forEach { chatId ->
//                    val chat = telegramDataSource.getChat(chatId)
//                    when (chat.type) {
//                        is ChatTypeSupergroup -> {
//                            val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
//                            if (telegramDataSource.getSuperGroupFullInfo(supergroupId).inviteLink.name == groupName) {
//                                return@executeSafety chatId
//                            }
//                        }
//                        is ChatTypeBasicGroup -> {
//                            val basicGroupInfo = telegramDataSource.getBasicGroupFullInfo(chatId)
//                            if (basicGroupInfo.inviteLink.name == groupName) {
//                                return@executeSafety chatId
//                            }
//                        }
//
//                        else -> {
//                            throw UnexpectedGroupType("Unexpected group type: ${chat.type}")
//                        }
//                    }
//                }
//            }
//            throw GroupIsNotFoundException("Group with name $groupName not found")
        }
    }

    override suspend fun addUsersToGroup(chatId: Long, usersIds: List<Long>): Result<Unit> {
        return executeSafety {
            logger("addUsersToGroup:", logLevel = LOGGER_LEVEL)
            telegramDataSource.inviteUserToGroup(chatId, usersIds.toLongArray())
        }
    }

    override suspend fun sendMessageToGroup(chatId: Long, message: String): Result<Unit> {
        return executeSafety {
            logger("sendMessageToGroup:", logLevel = LOGGER_LEVEL)
            telegramDataSource.sendMessageToGroup(chatId, message)
        }
    }
}
