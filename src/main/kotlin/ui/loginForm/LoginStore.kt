package ui.loginForm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.ConfigurationsRepositoryImpl
import data.model.LoginParamsData
import domain.models.LoginParams

class LoginStore(
    private val configs: ConfigurationsRepositoryImpl
) {
    var state: LoginParamsData by mutableStateOf(loginParams)
        private set


    private val loginParams: LoginParamsData
        get() = configs.getLoginData().getOrNull() ?: LoginParamsData("", "", "")

    fun editPhoneNumber(text: String) {
         configs.setLoginData(text, loginParams.apiId ?: "", loginParams.apiHash ?: "")
        setState {
            loginParams
        }
    }

    fun editApiId(text: String) {
        configs.setLoginData(loginParams.phoneNumber ?: "", text, loginParams.apiHash ?: "")
        setState {
            loginParams
        }
    }

    fun editApiHash(text: String) {
        configs.setLoginData(loginParams.phoneNumber ?: "", loginParams.apiId ?: "", text)
        setState {
            loginParams
        }
    }

    private inline fun setState(update: LoginParamsData.() -> LoginParamsData) {
        state = state.update()
    }
}
