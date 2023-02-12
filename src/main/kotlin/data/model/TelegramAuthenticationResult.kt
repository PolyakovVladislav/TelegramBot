package data.model

data class TelegramAuthenticationResult(
    val code: Int,
    val state: String
) {

    companion object {
        const val LOGGED_IN = 0
        const val CLOSING = 1
        const val CLOSED = 2
        const val LOGGING_OUT = 3
        const val WAITING_FOR_AUTH_CODE = 4
        const val WAITING_FOR_TDLIB_PARAMETERS = 5
        const val WAITING_FOR_PHONE_NUMBER = 6
    }
}
