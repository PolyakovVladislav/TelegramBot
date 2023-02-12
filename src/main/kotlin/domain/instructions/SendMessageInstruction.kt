package domain.instructions

import bot.Instruction
import domain.models.Message
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository

class SendMessageInstruction(
    private val telegramRepository: TelegramRepository,
    private val configs: ConfigurationsRepository,
    val message: Message,
    id: Int,
    executionTime: Long,
    description: String,
    onExecuted: (Instruction) -> Unit
) : Instruction(
    id,
    executionTime,
    0,
    0,
    30_000L,
    description,
    onExecuted
) {

    override suspend fun run() {
        val groupsForSpamResult = configs.getGroupsForSpam()
        if (groupsForSpamResult.isFailure) {
            throw requireNotNull(groupsForSpamResult.exceptionOrNull())
        }
        val groupsForSpam = groupsForSpamResult.getOrNull()!!
        groupsForSpam.forEach { telegramGroupData ->
            var chatId = telegramGroupData.chatId
            if (chatId == null) {
                val chatIdResult = telegramRepository.getChatId(telegramGroupData.groupLink)
                if (chatIdResult.isSuccess) {
                    telegramGroupData.chatId = chatIdResult.getOrNull()!!
                    chatId = telegramGroupData.chatId
                } else {
                    throw requireNotNull(chatIdResult.exceptionOrNull())
                }
            }
            telegramRepository.sendMessageToGroup(chatId!!, message.message)
        }
    }
}
