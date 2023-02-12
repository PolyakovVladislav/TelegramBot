package domain.instructions

import bot.Instruction
import data.model.TelegramAuthenticationResult
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository

class LoginInstruction(
    private val telegramRepository: TelegramRepository,
    private val configs: ConfigurationsRepository,
    private val onAuthCodeRequested: () -> Unit,
    private val authCode: String?,
    id: Int,
    executionTime: Long,
    description: String,
    onExecuted: (Instruction) -> Unit
) : Instruction(id, executionTime, 0, 5, 30_000L, description, onExecuted) {

    override suspend fun run() {
        println("LoginInstruction start")
        val loginParamsResult = configs.getLoginParams()
        if (loginParamsResult.isFailure) {
            throw requireNotNull(loginParamsResult.exceptionOrNull())
        }
        if (authCode != null) {
            val confirmationResult = telegramRepository.confirmLogin(authCode)
            if (confirmationResult.isFailure) {
                throw requireNotNull(confirmationResult.exceptionOrNull())
            }
        } else {
            val phoneNumber = loginParamsResult.getOrNull()!!.phoneNumber
            val apiId = loginParamsResult.getOrNull()!!.apiId
            val apiHash = loginParamsResult.getOrNull()!!.apiHash
            val loginResult = telegramRepository.login(phoneNumber, apiId, apiHash)
            if (loginResult.isFailure) {
                throw requireNotNull(loginResult.exceptionOrNull())
            }
            val login = loginResult.getOrNull()
            if (login?.code == TelegramAuthenticationResult.WAITING_FOR_AUTH_CODE || login?.code == TelegramAuthenticationResult.WAITING_FOR_PHONE_NUMBER) {
                onAuthCodeRequested()
            }
        }
        println("LoginInstruction finish")
    }
}
