package domain.instructions

import bot.Instruction
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository

class ScrapGroupInstruction(
    private val telegramRepository: TelegramRepository,
    private val configs: ConfigurationsRepository,
    id: Int,
    executionTime: Long,
    description: String,
    onExecuted: (Instruction) -> Unit
) : Instruction(
    id,
    executionTime,
    0,
    1,
    600_000L,
    description,
    onExecuted
) {

    override suspend fun run() {
        val groupsForScrapResult = configs.getGroupsForScrap()
        if (groupsForScrapResult.isFailure) {
            throw requireNotNull(groupsForScrapResult.exceptionOrNull())
        }
        val groupsForScrap = groupsForScrapResult.getOrNull()!!
        groupsForScrap.forEach { groupForScrap ->
            val scrapResult = telegramRepository.scrapGroupForUsersIds(groupForScrap.groupLink)
            if (scrapResult.isSuccess) {
                val usersIds = scrapResult.getOrNull()!!.usersIds.toMutableList()
                val scrapedUsersBeforeResult = configs.getScrapedUsersIds()
                if (scrapedUsersBeforeResult.isFailure) {
                    throw requireNotNull(scrapedUsersBeforeResult.exceptionOrNull())
                }
                val scrapedUsersBefore = scrapedUsersBeforeResult.getOrNull()!!
                usersIds.removeAll { id ->
                    scrapedUsersBefore.contains(id)
                }
                val groupsForSpamResult = configs.getGroupsForSpam()
                if (groupsForSpamResult.isFailure) {
                    throw requireNotNull(groupsForSpamResult.exceptionOrNull())
                }
                val groupsForSpam = groupsForSpamResult.getOrNull()!!
                groupsForSpam.forEach { groupForSpam ->
                    var chatId = groupForSpam.chatId
                    if (chatId == null) {
                        val getChatIdResult = telegramRepository.getChatId(groupForSpam.groupLink)
                        chatId = if (getChatIdResult.isFailure) {
                            throw requireNotNull(getChatIdResult.exceptionOrNull())
                        } else {
                            getChatIdResult.getOrNull()!!
                        }
                        groupForSpam.chatId = chatId
                    }
                    telegramRepository.addUsersToGroup(chatId, usersIds)
                    val currentUserIds = telegramRepository.scrapGroupForUsersIds(groupForSpam.groupLink)
                        .getOrNull()?.usersIds ?: listOf()
                    configs.addScrapedUsersIds(
                        currentUserIds.filter { currentUserId -> usersIds.contains(currentUserId) }
                    )
                }
            } else {
                throw requireNotNull(scrapResult.exceptionOrNull())
            }
        }
    }
}
