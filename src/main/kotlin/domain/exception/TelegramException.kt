package domain.exception

import java.lang.Exception

open class TelegramException(
    val code: Int,
    message: String
): Exception(message)
