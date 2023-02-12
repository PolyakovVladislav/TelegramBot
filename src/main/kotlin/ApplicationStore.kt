import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ApplicationStore {

    var state: AppState by mutableStateOf(
        AppState(
            false,
            null
        )
    )
        private set

    fun setExpectingAuthCode(expectingCode: Boolean) {
        state = AppState(
            expectingCode,
            state.authCode
        )
    }

    fun setAuthCode(authCode: String) {
        state = AppState(
            false,
            authCode
        )
    }

    fun codeApplied() {
        state = AppState(
            false,
            null
        )
    }

    data class AppState(
        var expectingAuthCode: Boolean,
        val authCode: String?
    )
}