package domain.instructions

import LOGGER_LEVEL
import bot.Instruction
import data.model.ScrapedUsers
import domain.models.Status
import domain.repositories.ConfigurationsRepository
import domain.repositories.TelegramRepository
import kotlinx.coroutines.flow.catch
import utils.Logger

class ScrapGroupInstruction(
    private val telegramRepository: TelegramRepository,
    private val configs: ConfigurationsRepository,
    id: Int,
    executionTime: Long,
    description: String,
    onExecuted: (Instruction) -> Unit,
    onProgressUpdated: (Status) -> Unit = { },
) : Instruction(
    id,
    executionTime,
    0,
    1,
    600_000L,
    description,
    onExecuted,
    onProgressUpdated,
) {

    companion object {
        private const val INSTRUCTION_TITLE_PREFIX = "Scrapping group:"
    }

    private val logger = Logger(this.javaClass.simpleName, configs, LOGGER_LEVEL)

    override suspend fun run() {
        val groupsForScrapResult = configs.getGroupsForScrap()
        if (groupsForScrapResult.isFailure) {
            throw requireNotNull(groupsForScrapResult.exceptionOrNull())
        }
        val groupsForScrap = groupsForScrapResult.getOrNull()!!
        groupsForScrap.forEach { groupForScrap ->
            val scrapFlow = telegramRepository.scrapGroupMembersIds(groupForScrap.groupLink)
            scrapFlow
                .catch { exception -> logger.e(exception) }
                .collect { scrapedUsers ->
                    if (scrapedUsers is ScrapedUsers.Progress) {
                        updateProgress(groupForScrap.groupLink, scrapedUsers)
                    } else if (scrapedUsers is ScrapedUsers.Result) {
                        val usersIds = scrapedUsers.usersIds.toMutableList()
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
                            val currentUserFlow = telegramRepository.scrapGroupMembersIds(groupForSpam.groupLink)
                            currentUserFlow
                                .catch { exception -> logger.e(exception) }
                                .collect { currentUsers ->
                                    if (currentUsers is ScrapedUsers.Result) {
                                        val currentUsersIds = currentUsers.usersIds
                                        configs.addScrapedUsersIds(
                                            currentUsersIds.filter { currentUserId -> usersIds.contains(currentUserId) },
                                        )
                                    }
                                }
                        }
                    }
                }
        }
    }

    private fun updateProgress(groupName: String, progress: ScrapedUsers.Progress) {
        onProgressUpdated(
            Status(
                "$INSTRUCTION_TITLE_PREFIX $groupName",
                "${progress.scrappedCount}/${progress.totalCount} = ${progress.progress}",
            ),
        )
    }
}
