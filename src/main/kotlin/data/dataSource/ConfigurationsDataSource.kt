package data.dataSource

import data.model.LoginParamsData
import data.model.MessageData
import data.model.TelegramGroupData
import domain.models.Message
import domain.models.TelegramGroup

interface ConfigurationsDataSource {

    companion object {
        const val MAIN_DIRECTORY = "Telegram spammer"
    }

    var loginParams: LoginParamsData

    var scrapedUsersIds: List<String>

    var groupsForScrap: List<TelegramGroupData>

    var groupForSpam: List<TelegramGroupData>

    var messages: List<MessageData>

    var scrapPeriod: String?

    var log: List<String>
}