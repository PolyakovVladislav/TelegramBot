package ui.scrapGroupsForm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.ConfigurationsRepositoryImpl
import data.dataSource.ConfigurationsDataSource
import domain.models.TelegramGroup
import domain.repositories.ConfigurationsRepository

class ScrapGroupsStore(
    private val configs: ConfigurationsRepository
) {

    var groupsState by mutableStateOf(scrapGroups)
        private set

    var groupNameState by mutableStateOf("")
        private set

    private val scrapGroups: List<TelegramGroup>
        get() = configs.getGroupsForScrap().getOrNull() ?: listOf()

    fun remove(groupName: String) {
        configs.removeGroupForScrap(groupName)
        setState {
            scrapGroups
        }
    }

    fun add(groupName: String) {
        configs.addGroupForScrap(groupName)
        setState {
            scrapGroups
        }
    }

    private inline fun setState(update: List<TelegramGroup>.() -> List<TelegramGroup>) {
        groupsState = groupsState.update()
    }

    private inline fun setFieldState(update: String.() -> String) {
        groupNameState = groupNameState.update()
    }

    fun editGroupName(text: String) {
        setFieldState {
            text
        }
    }
}
