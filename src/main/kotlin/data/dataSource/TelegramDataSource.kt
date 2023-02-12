package data.dataSource

import domain.exception.TelegramException
import data.model.TelegramAuthenticationResult
import it.tdlight.jni.TdApi.BasicGroupFullInfo
import it.tdlight.jni.TdApi.Chat
import it.tdlight.jni.TdApi.ChatMembers
import it.tdlight.jni.TdApi.Chats
import it.tdlight.jni.TdApi.SupergroupFullInfo

interface TelegramDataSource {

    @Throws(TelegramException::class)
    suspend fun login(phoneNumber: Long, apiId: Int, apiHash: String): TelegramAuthenticationResult

    @Throws(TelegramException::class)
    suspend fun confirmAuthorizationWithCode(authcode: String)

    @Throws(TelegramException::class)
    suspend fun inviteUserToGroup(chatId: Long, users: LongArray)

    @Throws(TelegramException::class)
    suspend fun sendMessageToGroup(chatId: Long, message: String)

    @Throws(TelegramException::class)
    suspend fun searchPublicChat(groupLink: String): Chat

    @Throws(TelegramException::class)
    suspend fun searchPublicChats(groupLink: String): Chats

    @Throws(TelegramException::class)
    suspend fun getChat(chatId: Long): Chat

    @Throws(TelegramException::class)
    suspend fun getSuperGroupFullInfo(supergroupId: Long): SupergroupFullInfo

    @Throws(TelegramException::class)
    suspend fun getBasicGroupFullInfo(chatId: Long): BasicGroupFullInfo

    @Throws(TelegramException::class)
    suspend fun getSuperGroupMembers(supergroupId: Long, offset: Int, limit: Int): ChatMembers
}