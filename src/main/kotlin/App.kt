import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition.Aligned
import androidx.compose.ui.window.WindowState
import bot.Bot
import bot.Instruction
import domain.instructions.LoginInstruction
import domain.instructions.ScrapGroupInstruction
import domain.instructions.SendMessageInstruction
import domain.models.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ui.loginForm.LoginForm
import ui.messages.MessagesForm
import ui.myGroupsForm.MyGroupForm
import ui.scrapGroupsForm.ScrapGroupsForm
import ui.statusForm.StatusForm
import ui.statusForm.StatusStore
import utils.Logger

val bot = Bot(appScope, configurationRepository, ::onInstructionTimeout, ::onInstructionFailed)
private val logger = Logger("App", configurationRepository, LOGGER_LEVEL)

@Preview
@Composable
fun App() {
    val appState = remember {
        ApplicationStore()
    }
    val statusState = remember {
        StatusStore()
    }

    val mCheckedState = remember { mutableStateOf(false) }

    val data = runBlocking {
        val scrapPeriodResult = configurationRepository.getScrapPeriodMillis()
        if (scrapPeriodResult.isSuccess) {
            (scrapPeriodResult.getOrNull()!! / 60 / 1000).toString()
        } else {
            ""
        }
    }

    val scrapPeriod = remember {
        mutableStateOf(
            data,
        )
    }

    MaterialTheme {
        Column {
            Row(Modifier.fillMaxWidth()) {
                LoginForm()
                Switch(
                    checked = mCheckedState.value,
                    onCheckedChange = {
                        mCheckedState.value = it
                        onToggleBot(
                            it,
                            appState,
                            statusState
                        )
                    },
                )
                Column {
                    OutlinedTextField(
                        value = scrapPeriod.value,
                        onValueChange = {
                            scrapPeriod.value = it
                            if (it != "") {
                                try {
                                    configurationRepository.setScrapPeriodMillis(it.toLong() * 60 * 1000)
                                } catch (e: Exception) {
                                    logger.e(e)
                                }
                            }
                        },
                        label = { Text("Scrap period (minutes)") },
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    StatusForm(
                        statusState,
                    )
                }
            }

            Row {
                ScrapGroupsForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f))
                MyGroupForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f))
                MessagesForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f)) {
                    onMessagesChanged(statusState)
                }
            }
        }
    }

    if (appState.state.expectingAuthCode.not()) {
        Window(
            title = "Enter auth code",
            alwaysOnTop = true,
            resizable = false,
            onCloseRequest = {
                appState.setExpectingAuthCode(false)
                mCheckedState.value = false
                onToggleBot(
                    false,
                    appState,
                    statusState
                )
            },
            state = WindowState(
                position = Aligned(Alignment.Center),
                size = DpSize(300.dp, 160.dp),
            ),
        ) {
            AuthCodeWindow { authCode ->
                appState.setAuthCode(authCode)
            }
        }
    }
    if (appState.state.authCode != null) {
        onAuthEntered(appState.state.authCode!!, appState, statusState)
        appState.codeApplied()
    }
}

fun onInstructionFailed(exception: Exception, instruction: Instruction) {
    logger.e(exception)
}

private fun onInstructionTimeout(exception: Exception) {
    logger.e(exception)
}

private fun onMessagesChanged(statusStore: StatusStore) {
    val messageInstruction = try {
        getSendInstruction(statusStore)
    } catch (e: Exception) {
        logger.e(e)
        return
    }
    if (bot.isAnyInstructionExecuting()) {
        val runningInstruction = bot.getInstructions().firstOrNull()
            ?: bot.add(messageInstruction)
        if (runningInstruction !is SendMessageInstruction) {
            bot.getInstructions().find { it is SendMessageInstruction }?.let { bot.remove(it) }
            bot.add(messageInstruction)
        }
    } else {
        bot.getInstructions().find { it is SendMessageInstruction }?.let { bot.remove(it) }
        bot.add(messageInstruction)
    }
}

private fun onLoginInstructionExecuted(instruction: Instruction, statusStore: StatusStore) {
    logger("onLoginInstructionExecuted:")
    bot.add(createScrapInstruction(System.currentTimeMillis(), statusStore))
    try {
        val sendMessageInstruction = getSendInstruction(statusStore)
        bot.add(sendMessageInstruction)
    } catch (e: Exception) {
        logger.e(e)
    }
}

private fun onToggleBot(toggle: Boolean, appState: ApplicationStore, statusStore: StatusStore) {
    if (toggle) {
        bot.start()
        bot.add(
            createLoginInstruction(null, appState, statusStore),
        )
    } else {
        bot.stop(true)
        for (instruction in bot.getInstructions()) {
            bot.remove(instruction)
        }
    }
}

private fun onScrapInstructionExecuted(instruction: Instruction, statusStore: StatusStore) {
    bot.add(
        createScrapInstruction(
            System.currentTimeMillis() + configurationRepository.getScrapPeriodMillis().getOrNull()!!,
            statusStore,
        ),
    )
}

private fun onSendMessageInstructionExecuted(instruction: Instruction, statusStore: StatusStore) {
    instruction as SendMessageInstruction
    configurationRepository.markMessageAsSent(instruction.message)
    try {
        bot.add(getSendInstruction(statusStore))
    } catch (e: Exception) {
        logger.e(e)
        appScope.launch {
            delay(30000)
            onSendMessageInstructionExecuted(instruction, statusStore)
        }
    } catch (e: Exception) {
        logger.e(e)
    }
}

private fun createLoginInstruction(
    authCode: String?,
    appState: ApplicationStore,
    statusStore: StatusStore,
): LoginInstruction {
    return LoginInstruction(
        telegramRepository,
        configurationRepository,
        { onAuthRequested(appState) },
        authCode,
        LOGIN_INSTRUCTION_ID,
        0,
        "Login instruction"
    ) { instruction ->
        onLoginInstructionExecuted(instruction, statusStore)
    }
}

private fun onAuthRequested(appState: ApplicationStore) {
    appState.setExpectingAuthCode(true)
    bot.stop(true)
    bot.remove(0)
}

private fun onAuthEntered(authCode: String, appState: ApplicationStore, statusStore: StatusStore) {
    bot.add(
        createLoginInstruction(authCode, appState, statusStore),
    )
    bot.start()
}

private fun createScrapInstruction(executionTime: Long, statusStore: StatusStore): ScrapGroupInstruction {
    return ScrapGroupInstruction(
        telegramRepository,
        configurationRepository,
        SCRAP_GROUP_INSTRUCTION_ID,
        executionTime,
        "Scrap group for users instructions",
        { instruction ->  onScrapInstructionExecuted(instruction, statusStore) },
    ) { status ->
        onInstructionStatusUpdate(status, statusStore)
    }
}

fun onInstructionStatusUpdate(status: Status, statusStore: StatusStore) {
    statusStore.setStatus(status)
}

private fun getSendInstruction(statusStore: StatusStore): SendMessageInstruction {
    val messagesResult = configurationRepository.getMessages()
    if (messagesResult.isFailure) {
        throw requireNotNull(messagesResult.exceptionOrNull())
    } else if (messagesResult.getOrNull()!!.isEmpty()) {
        throw IllegalStateException("Can't find closest message.\n Messages = ${messagesResult.getOrNull()}")
    }
    val message = messagesResult.getOrNull()!!
        .filter { message ->
            if (message.lastSentTimestamp == null) {
                true
            } else {
                System.currentTimeMillis() - 60000 > message.lastSentTimestamp
            }
        }
        .minByOrNull { message ->
            message.rtcTime
        } ?: throw IllegalStateException("Can't find closest message.\n Messages = ${messagesResult.getOrNull()}")

    return SendMessageInstruction(
        telegramRepository,
        configurationRepository,
        message,
        SEND_MESSAGE_INSTRUCTION_ID,
        message.rtcTime,
        "Send message",
        { instruction -> onSendMessageInstructionExecuted(instruction, statusStore) },
    ) { status ->
        onInstructionStatusUpdate(status, statusStore)
    }
}
