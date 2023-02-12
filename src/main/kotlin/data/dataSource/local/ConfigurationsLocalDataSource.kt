package data.dataSource.local

import data.dataSource.ConfigurationsDataSource
import data.dataSource.ConfigurationsDataSource.Companion.MAIN_DIRECTORY
import data.model.LoginParamsData
import data.model.MessageData
import data.model.TelegramGroupData
import utils.search
import java.io.File

class ConfigurationsLocalDataSource : ConfigurationsDataSource {

    companion object {
        private const val END_TAG = "END_TAG"

        private const val PHONE_NUMBER = "PHONE_NUMBER"
        private const val API_ID = "API_ID"
        private const val API_HASH = "API_HASH"

        private const val CHAT_ID = "CHAT_ID"
        private const val ID = "ID"
        private const val NAME = "NAME"

        private const val MESSAGE = "MESSAGE"
        private const val TIME = "TIME"

        private const val TIMESTAMP = "TIMESTAMP"

        val CONFIG_DIRECTORY = File("${System.getProperty("user.home")}\\AppData\\Local\\$MAIN_DIRECTORY")
    }

    private val loginDataFile = File("$CONFIG_DIRECTORY\\loginData.txt")
    private val scrapedUsersFile = File("$CONFIG_DIRECTORY\\scrapedUsers.txt")
    private val groupsForScrapFile = File("$CONFIG_DIRECTORY\\groupsForScrap.txt")
    private val groupsForSpamFile = File("$CONFIG_DIRECTORY\\groupsForSpam.txt")
    private val messagesFile = File("$CONFIG_DIRECTORY\\messages.txt")
    private val scanPeriodFile = File("$CONFIG_DIRECTORY\\scanPeriod.txt")
    private val logFile = File("$CONFIG_DIRECTORY\\log.txt")

    init {
        if (CONFIG_DIRECTORY.exists().not()) {
            CONFIG_DIRECTORY.mkdirs()
        }
    }

    private val defaultLoginParams = LoginParamsData(
        null,
        null,
        null
    )
    override var loginParams: LoginParamsData
        get() {
            val list = readFile(loginDataFile) ?: return defaultLoginParams
            val line = list.firstOrNull() ?: return defaultLoginParams
            val phoneNumber = line.getValue(PHONE_NUMBER, END_TAG)
            val apiId = line.getValue(API_ID, END_TAG)
            val apiHash = line.getValue(API_HASH, END_TAG)
            return LoginParamsData(
                phoneNumber,
                apiId,
                apiHash
            )
        }
        set(value) {
            val list = listOf(
                "$PHONE_NUMBER${value.phoneNumber}$END_TAG " +
                    "$API_ID${value.apiId}$END_TAG " +
                    "$API_HASH${value.apiHash}$END_TAG"
            )

            writeFile(
                loginDataFile,
                list
            )
        }

    override var scrapedUsersIds: List<String>
        get() {
            val list = readFile(scrapedUsersFile) ?: return listOf()
            return list.map { line ->
                line
            }
        }
        set(value) {
            writeFile(scrapedUsersFile, value.map { it })
        }

    override var groupsForScrap: List<TelegramGroupData>
        get() {
            val list = readFile(groupsForScrapFile) ?: return listOf()
            return list.map { line ->
                val name = line.getValue(NAME, END_TAG)
                val chatId = line.getValue(CHAT_ID, END_TAG)
                TelegramGroupData(
                    name,
                    chatId
                )
            }
        }
        set(value) {
            val list = value.map { telegramGroupData ->
                "$NAME${telegramGroupData.name}$END_TAG " +
                    "$CHAT_ID${telegramGroupData.chatId}$END_TAG"
            }
            writeFile(
                groupsForScrapFile,
                list
            )
        }

    override var groupForSpam: List<TelegramGroupData>
        get() {
            val list = readFile(groupsForSpamFile) ?: return listOf()
            return list.map { line ->
                val name = line.getValue(NAME, END_TAG)
                val chatId = line.getValue(CHAT_ID, END_TAG)
                TelegramGroupData(
                    name,
                    chatId
                )
            }
        }
        set(value) {
            val list = value.map { telegramGroupData ->
                "$NAME${telegramGroupData.name}$END_TAG " +
                    "$CHAT_ID${telegramGroupData.chatId}$END_TAG"
            }
            writeFile(
                groupsForSpamFile,
                list
            )
        }

    override var messages: List<MessageData>
        get() {
            val list = readFile(messagesFile) ?: return listOf()
            return list.map { line ->
                val id = line.getValue(ID, END_TAG)
                val message = line.getValue(MESSAGE, END_TAG)
                val time = line.getValue(TIME, END_TAG)
                val lastSentTimestamp = line.getValue(TIMESTAMP, END_TAG)
                MessageData(
                    id,
                    message,
                    time,
                    lastSentTimestamp
                )
            }
        }
        set(value) {
            val list = value.map { message ->
                "$ID${message.id}$END_TAG " +
                    "$MESSAGE${message.message}$END_TAG " +
                    "$TIME${message.time}$END_TAG " +
                    "$TIMESTAMP${message.lastSentTimestamp}$END_TAG"
            }
            writeFile(
                messagesFile,
                list
            )
        }

    override var scrapPeriod: String?
        get() {
            return readFile(scanPeriodFile)?.firstOrNull()
        }
        set(value) {
            writeFile(scanPeriodFile, listOf(value.toString()))
        }

    override var log: List<String>
        get() = readFile(logFile) ?: emptyList()
        set(value) {
            writeFile(logFile, value)
        }

    private fun writeFile(file: File, list: List<String>) {
        val writer = file.bufferedWriter()
        writer.flush()
        list.forEachIndexed { index, line ->
            writer.write(line)
            if (list.size != index + 1) {
                writer.newLine()
            }
        }
        writer.close()
    }

    private fun readFile(file: File): List<String>? {
        return if (file.exists()) {
            file.readLines()
        } else {
            null
        }
    }

    private fun String.getValue(startTag: String, endTag: String): String? {
        val result = search(startTag, endTag)
        return if (result == "null") null else result
    }
}
