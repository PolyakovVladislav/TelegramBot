package domain.repositories

import data.model.LoginParamsData
import domain.models.LoginParams
import domain.models.Message
import domain.models.TelegramGroup

interface ConfigurationsRepository {

     fun getLoginParams(): Result<LoginParams>

     fun getLoginData(): Result<LoginParamsData>

     fun setLoginData(phoneNumber: String, apiId: String, apiHash: String): Result<Unit>

     fun getScrapedUsersIds(): Result<List<Long>>

     fun getGroupsForScrap(): Result<List<TelegramGroup>>

     fun getGroupsForSpam(): Result<List<TelegramGroup>>

     fun getMessages(): Result<List<Message>>

     fun markMessageAsSent(message: Message): Result<Unit>

     fun getScrapPeriodMillis(): Result<Long>

     fun setLoginParams(phoneNumber: String, apiId: String, apiHash: String): Result<Unit>

     fun addScrapedUsersIds(list: List<Long>): Result<Unit>

     fun removeGroupForScrap(name: String): Result<Unit>

     fun addGroupForScrap(name: String, chatId: Long? = null): Result<Unit>

     fun removeGroupForSpam(name: String): Result<Unit>

     fun addGroupForSpam(name: String, chatId: Long? = null): Result<Unit>

     fun removeMessage(id: Long): Result<Unit>

     fun addMessage(message: String, time: String): Result<Unit>

     fun setScrapPeriodMillis(periodMillis: Long): Result<Unit>

     fun log(rows: List<String>): Result<Unit>
}
