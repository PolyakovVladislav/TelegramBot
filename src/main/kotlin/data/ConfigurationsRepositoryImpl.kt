package data

import LOGGER_LEVEL
import data.dataSource.ConfigurationsDataSource
import data.model.LoginParamsData
import data.model.MessageData
import data.model.TelegramGroupData
import domain.models.LoginParams
import domain.models.Message
import domain.models.TelegramGroup
import domain.repositories.ConfigurationsRepository
import utils.Logger

class ConfigurationsRepositoryImpl(
    private val configs: ConfigurationsDataSource
) : ConfigurationsRepository {

    private val logger = Logger(this.javaClass.simpleName, this, LOGGER_LEVEL)

    private fun <T> executeSafety(action: () -> T): Result<T> {
        return try {
            Result.success(action())
        } catch (e: Exception) {
            logger.e(e)
            Result.failure(e)
        }
    }

    override fun getLoginParams(): Result<LoginParams> {
        return executeSafety {
            val data = configs.loginParams
            if (data.phoneNumber == null || data.phoneNumber?.matches(Regex("^\\d+\$")) == false) {
                throw IllegalArgumentException("Wrong phone number format: ${data.phoneNumber}. Should contain only numbers")
            }
            if (data.apiId == null || data.apiId?.matches(Regex("^\\d+\$")) == false) {
                throw IllegalArgumentException("Wrong api id format: ${data.apiId}. Should contain only numbers")
            }
            if (data.apiHash == null || data.apiHash?.matches(Regex("^.+$")) == false) {
                throw IllegalArgumentException("Wrong api hash: ${data.apiHash}")
            }
            LoginParams(
                data.phoneNumber?.toLong()!!,
                data.apiId?.toInt()!!,
                data.apiHash!!
            )
        }
    }

    override fun getLoginData(): Result<LoginParamsData> {
        return executeSafety {
            configs.loginParams
        }
    }

    override fun setLoginData(phoneNumber: String, apiId: String, apiHash: String): Result<Unit> {
        return executeSafety {
            configs.loginParams = LoginParamsData(phoneNumber, apiId, apiHash)
        }
    }

    override fun getScrapedUsersIds(): Result<List<Long>> {
        return executeSafety {
            val data = configs.scrapedUsersIds
            val list = mutableListOf<Long>()
            data.forEach { id ->
                try {
                    if (!id.matches(Regex("^\\d+\$"))) {
                        throw IllegalArgumentException("Illegal scrapped user id: $id")
                    } else {
                        list.add(id.toLong())
                    }
                } catch (e: IllegalArgumentException) {
                    logger.e(e)
                }
            }
            list
        }
    }

    override fun getGroupsForScrap(): Result<List<TelegramGroup>> {
        return executeSafety {
            val data = configs.groupsForScrap
            val list = mutableListOf<TelegramGroup>()
            data.forEach { telegramGroupData ->
                try {
                    if (telegramGroupData.chatId != null && !telegramGroupData.chatId!!.matches(Regex("^\\d+\$"))) {
                        throw IllegalArgumentException("Illegal chat id number: ${telegramGroupData.chatId}")
                    } else if (telegramGroupData.name == null) {
                        throw IllegalStateException("Group data can't be null")
                    } else {
                        list.add(
                            TelegramGroup(
                                telegramGroupData.name,
                                telegramGroupData.chatId?.toLong()
                            )
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    list.add(
                        TelegramGroup(
                            telegramGroupData.name!!,
                            null
                        )
                    )
                    logger.e(e)
                } catch (e: IllegalStateException) {
                    logger.e(e)
                }
            }
            list
        }
    }

    override fun getGroupsForSpam(): Result<List<TelegramGroup>> {
        return executeSafety {
            val data = configs.groupForSpam
            val list = mutableListOf<TelegramGroup>()
            data.forEach { telegramGroupData ->
                try {
                    if (telegramGroupData.chatId != null && !telegramGroupData.chatId!!.matches(Regex("^\\d+\$"))) {
                        throw IllegalArgumentException("Illegal chat id number: ${telegramGroupData.chatId}")
                    } else if (telegramGroupData.name == null) {
                        throw IllegalStateException("Group data can't be null")
                    } else {
                        list.add(
                            TelegramGroup(
                                telegramGroupData.name,
                                telegramGroupData.chatId?.toLong()
                            )
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    list.add(
                        TelegramGroup(
                            telegramGroupData.name!!,
                            null
                        )
                    )
                    logger.e(e)
                } catch (e: IllegalStateException) {
                    logger.e(e)
                }
            }
            list
        }
    }

    override fun getMessages(): Result<List<Message>> {
        return executeSafety {
            val data = configs.messages
            val list = mutableListOf<Message>()
            data.forEach { messageData ->
                try {
                    if (messageData.message == null) {
                        throw IllegalArgumentException("Message text can't be null")
                    }
                    if (messageData.id?.matches(Regex("^\\d+$")) == false) {
                        throw IllegalArgumentException("Wrong message id data: ${messageData.id}")
                    }
                    if (messageData.time?.matches(Regex("^\\d+:\\d+")) == false) {
                        throw IllegalArgumentException("Wrong time data: ${messageData.time}")
                    }
                    if (messageData.lastSentTimestamp?.matches(Regex("^\\d+$")) == false ||
                        messageData.lastSentTimestamp == "null"
                    ) {
                        throw IllegalArgumentException("Wrong timestamp: ${messageData.lastSentTimestamp}")
                    }
                    val lastSentTimestamp = if (messageData.lastSentTimestamp == "null" || messageData.lastSentTimestamp == null) {
                        null
                    } else {
                        messageData.lastSentTimestamp!!.toLong()
                    }
                    list.add(
                        Message(
                            messageData.id?.toLong()!!,
                            messageData.message,
                            messageData.time!!,
                            lastSentTimestamp
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    logger.e(e)
                }
            }
            list
        }
    }

    override fun markMessageAsSent(message: Message): Result<Unit> {
        return Result.success(
            kotlin.run {
                val storedMessages = configs.messages.toMutableList()
                storedMessages.find { messageData ->
                    messageData.id == message.id.toString()
                }?.lastSentTimestamp = System.currentTimeMillis().toString()
                configs.messages = storedMessages
            }
        )
    }

    override fun getScrapPeriodMillis(): Result<Long> {
        return executeSafety {
            val data = configs.scrapPeriod
            if (data?.matches(Regex("^\\d+$")) == true) {
                data.toLong()
            } else {
                600_000
            }
        }
    }

    override fun setLoginParams(phoneNumber: String, apiId: String, apiHash: String): Result<Unit> {
        return executeSafety {
            if (!phoneNumber.matches(Regex("^\\d+\$"))) {
                throw IllegalArgumentException("Wrong phone number format: $phoneNumber. Should contain only numbers")
            }
            if (!apiId.matches(Regex("^\\d+\$"))) {
                throw IllegalArgumentException("Wrong api id format: $apiId. Should contain only numbers")
            }
            if (!apiHash.matches(Regex("^.+&"))) {
                throw IllegalArgumentException("Wrong api hash: $apiHash")
            }
            configs.loginParams = LoginParamsData(
                phoneNumber,
                apiId,
                apiHash
            )
        }
    }

    override fun addScrapedUsersIds(list: List<Long>): Result<Unit> {
        return executeSafety {
            val storedScrapedIds = configs.scrapedUsersIds
            val ids = (storedScrapedIds + list.map { it.toString() }).distinct()
            configs.scrapedUsersIds = ids
        }
    }

    override fun removeGroupForScrap(name: String): Result<Unit> {
        return executeSafety {
            val storedGroups = configs.groupsForScrap.toMutableList()
            storedGroups.removeIf { it.name == name }
            configs.groupsForScrap = storedGroups
        }
    }

    override fun addGroupForScrap(name: String, chatId: Long?): Result<Unit> {
        return executeSafety {
            val storedGroups = configs.groupsForScrap.toMutableList()
            storedGroups.add(
                TelegramGroupData(
                    name,
                    chatId?.toString()
                )
            )
            configs.groupsForScrap = storedGroups
        }
    }

    override fun removeGroupForSpam(name: String): Result<Unit> {
        return executeSafety {
            val storedGroups = configs.groupForSpam.toMutableList()
            storedGroups.removeIf { it.name == name }
            configs.groupForSpam = storedGroups
        }
    }

    override fun addGroupForSpam(name: String, chatId: Long?): Result<Unit> {
        return executeSafety {
            val storedGroups = configs.groupForSpam.toMutableList()
            storedGroups.add(
                TelegramGroupData(
                    name,
                    chatId?.toString()
                )
            )
            configs.groupForSpam = storedGroups
        }
    }

    override fun removeMessage(id: Long): Result<Unit> {
        return executeSafety {
            val storedMessages = configs.messages.toMutableList()
            storedMessages.removeIf { it.id == id.toString() }
            configs.messages = storedMessages
        }
    }

    override fun addMessage(message: String, time: String): Result<Unit> {
        return executeSafety {
            val storedMessages = configs.messages.toMutableList()
            val latestId = storedMessages.maxByOrNull { it.id?.toLong() ?: -1 }?.id?.toLong() ?: -1
            storedMessages.add(
                MessageData((latestId + 1).toString(), message, time, null)
            )
            configs.messages = storedMessages
        }
    }

    override fun setScrapPeriodMillis(periodMillis: Long): Result<Unit> {
        return executeSafety {
            configs.scrapPeriod = periodMillis.toString()
        }
    }

    override fun log(rows: List<String>): Result<Unit> {
        return Result.success(
            kotlin.run {
                val log = configs.log
                val possibleIndexesBegin = log.size - 2000
                val possibleIndexesEnd = log.size
                val newLog = log.filterIndexed { index, _ ->
                    index in possibleIndexesBegin..possibleIndexesEnd
                }
                configs.log = newLog + rows
            }
        )
    }
}
