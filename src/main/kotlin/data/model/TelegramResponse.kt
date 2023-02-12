package data.model

import it.tdlight.jni.TdApi.Object

data class TelegramResponse(
    val result: Object,
    val error: String,
    val telegramError: String,
    val data: Any? = null
)
