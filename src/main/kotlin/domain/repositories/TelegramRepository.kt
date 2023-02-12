package domain.repositories

import data.model.ScrapUsersResult
import data.model.TelegramAuthenticationResult

interface TelegramRepository {

    suspend fun login(phoneNumber: Long, apiId: Int, apiHash: String): Result<TelegramAuthenticationResult>

    suspend fun confirmLogin(authCode: String): Result<Unit>

    suspend fun scrapGroupForUsersIds(groupLink: String): Result<ScrapUsersResult>

    suspend fun addUsersToGroup(chatId: Long, usersIds: List<Long>): Result<Unit>

    suspend fun getChatId(groupLink: String): Result<Long>

    suspend fun sendMessageToGroup(chatId: Long, message: String): Result<Unit>
}