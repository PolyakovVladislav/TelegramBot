package ui.statusForm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import domain.models.Status

class StatusStore {

    var statusState by mutableStateOf<Status?>(null)
        private set

    fun setStatus(status: Status) {
        statusState = status
    }

    fun resetStatus() {
        statusState = Status()
    }
}