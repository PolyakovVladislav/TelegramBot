package domain.models

data class TelegramGroup(
    val groupLink: String,
    var chatId: Long? = null
)
