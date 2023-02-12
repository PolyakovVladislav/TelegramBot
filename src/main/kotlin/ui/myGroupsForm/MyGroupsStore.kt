package ui.myGroupsForm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import domain.models.TelegramGroup
import domain.repositories.ConfigurationsRepository

class MyGroupsStore(
    private val configs: ConfigurationsRepository
) {

    var groupsStore by mutableStateOf(myGroups)
        private set
    var groupNameState by mutableStateOf("")
        private set

    private val myGroups: List<TelegramGroup>
        get() = configs.getGroupsForSpam().getOrNull()!!

    fun remove(name: String) {
        configs.removeGroupForSpam(name)
        setState {
            myGroups
        }
    }

    fun add(groupName: String) {
        configs.addGroupForSpam(groupName)
        setState {
            myGroups
        }
    }

    private inline fun setState(update: List<TelegramGroup>.() -> List<TelegramGroup>) {
        groupsStore = groupsStore.update()
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
