package data

import LOGGER_LEVEL
import data.dataSource.TelegramDataSource
import data.dataSource.remote.TelegramRemoteDataSource
import data.model.ScrapedUsers
import data.model.TelegramAuthenticationResult
import domain.exception.TelegramException
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository
import it.tdlight.jni.TdApi.Chat
import it.tdlight.jni.TdApi.ChatTypeBasicGroup
import it.tdlight.jni.TdApi.ChatTypeSupergroup
import it.tdlight.jni.TdApi.MessageSenderUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import utils.Logger
import kotlin.math.floor

class TelegramRepositoryImpl(
    private val telegramDataSource: TelegramDataSource,
    private val configs: ConfigurationsRepository,
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

    override suspend fun scrapGroupForUsersIds(groupLink: String): Flow<ScrapedUsers> {
        logger("scrapGroupForUsersIds:", logLevel = LOGGER_LEVEL)
        val chat = telegramDataSource.searchPublicChat(groupLink)
        return when (chat.type) {
            is ChatTypeSupergroup -> scarpUserIdsFromSuperGroup(chat)

            is ChatTypeBasicGroup -> scrapUserIdsFromBasicGroup(chat)

            else -> {
                throw TelegramException(
                    TelegramRemoteDataSource.UNSUPPORTED_GROUP_TYPE,
                    "Unsupported group type: ${chat.type}",
                )
            }
        }
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
                    "Unsupported group type: ${chat.type}",
                )
            }
        }
    }

    private suspend fun scarpUserIdsFromSuperGroup(chat: Chat): Flow<ScrapedUsers> {
        return flow {
            val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
            val superGroupInfo = telegramDataSource.getSuperGroupFullInfo(supergroupId)
            val membersCount = superGroupInfo.memberCount
            if (superGroupInfo.canGetMembers) {
                var iMax = if (membersCount <= 200) {
                    0
                } else {
                    floor(membersCount / 200f).toInt()
                }
                val limit = 200
                val list = mutableListOf<Long>()
                for (i in 0..iMax) {
                    delay(1000)
                    val offset = i * 200
                    val members = try {
                        telegramDataSource.getSuperGroupMembers(supergroupId, offset, limit)
                    } catch (e: TelegramException) {
                        emit(
                            ScrapedUsers.Result(
                                list,
                                e,
                            ),
                        )
                        return@flow
                    }
                    list.addAll(
                        (members.members.map { (it.memberId as MessageSenderUser).userId }),
                    )
                    if (members.totalCount <= offset + limit) {
                        break
                    }
                }
                emit(
                    ScrapedUsers.Result(
                        list,
                        null,
                    ),
                )
            } else {
                throw TelegramException(
                    TelegramRemoteDataSource.GROUP_CANT_BE_SCRAPED,
                    "Group ${superGroupInfo.inviteLink} can't be scrapped by group restrictions",
                )
            }
        }
    }

    private suspend fun scrapUserIdsFromBasicGroup(chat: Chat): Flow<ScrapedUsers> {
        return flow {
            val basicGroupInfo = telegramDataSource.getBasicGroupFullInfo(chat.id)
            val ids = basicGroupInfo.members.map { member ->
                (member.memberId as MessageSenderUser).userId
            }
            emit(ScrapedUsers.Result(ids, null))
        }
    }

    override suspend fun getChatId(groupLink: String): Result<Long> {
        return executeSafety {
            logger("getChatId:", logLevel = LOGGER_LEVEL)
            val searchResult = telegramDataSource.searchPublicChat(groupLink)
            return@executeSafety searchResult.id
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
