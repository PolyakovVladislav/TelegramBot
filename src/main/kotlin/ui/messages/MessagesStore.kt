package ui.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import domain.models.Message
import domain.models.TelegramGroup
import domain.repositories.ConfigurationsRepository

class MessagesStore(
    private val configs: ConfigurationsRepository
) {

    var state by mutableStateOf(messages)
        private set

    var messageState by mutableStateOf("")
        private set
    var timeState by mutableStateOf("")

    private val messages: List<Message>
        get() = configs.getMessages().getOrNull()!!

    fun remove(id: Long) {
        configs.removeMessage(id)
        setState {
            messages
        }
    }

    fun add(message: String, time: String) {
        configs.addMessage(message, time)
        setState {
            messages
        }
    }

    private inline fun setState(update: List<Message>.() -> List<Message>) {
        state = state.update()
    }

    private inline fun setMessageState(update: String.() -> String) {
        messageState = messageState.update()
    }

    private inline fun setTimeState(update: String.() -> String) {
        timeState = timeState.update()
    }

    fun editMessageField(text: String) {
        setMessageState {
            text
        }
    }

    fun editTimeField(text: String) {
        setTimeState {
            text
        }
    }
}
