import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ui.loginForm.LoginForm
import ui.messages.MessagesForm
import ui.myGroupsForm.MyGroupForm
import ui.scrapGroupsForm.ScrapGroupsForm
import utils.Logger

val bot = Bot(appScope, configurationRepository, ::onInstructionTimeout, ::onInstructionFailed)
private val logger = Logger("App", configurationRepository, LOGGER_LEVEL)

@Composable
@Preview
fun App() {
    val appState = remember {
        ApplicationStore()
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
            data
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
                            appState
                        )
                    }
                )
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
                    label = { Text("Scrap period (minutes)") }
                )
            }
            Row {
                ScrapGroupsForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f))
                MyGroupForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f))
                MessagesForm(modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f), ::onMessagesChanged)
            }
        }
    }

    if (appState.state.expectingAuthCode) {
        Window(
            title = "Enter auth code",
            onCloseRequest = {
                appState.setExpectingAuthCode(false)
                mCheckedState.value = false
                onToggleBot(
                    false,
                    appState
                )
            },
            state = WindowState(
                position = Aligned(Alignment.Center),
                size = DpSize(300.dp, 200.dp)
            )
        ) {
            AuthCodeWindow { authCode ->
                appState.setAuthCode(authCode)
            }
        }
    }
    if (appState.state.authCode != null) {
        onAuthEntered(appState.state.authCode!!, appState)
        appState.codeApplied()
    }
}

fun onInstructionFailed(exception: Exception, instruction: Instruction) {
    logger.e(exception)
}

private fun onInstructionTimeout(exception: Exception) {
    logger.e(exception)
}

private fun onMessagesChanged() {
    val messageInstruction = try {
        getSendInstruction()
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

private fun onLoginInstructionExecuted(instruction: Instruction) {
    logger("onLoginInstructionExecuted:")
    bot.add(createScrapInstruction(System.currentTimeMillis()))
    try {
        val sendMessageInstruction = getSendInstruction()
        bot.add(sendMessageInstruction)
    } catch (e: Exception) {
        logger.e(e)
    }
}

private fun onToggleBot(toggle: Boolean, appState: ApplicationStore) {
    if (toggle) {
        bot.start()
        bot.add(
            createLoginInstruction(null, appState)
        )
    } else {
        bot.stop(true)
        for (instruction in bot.getInstructions()) {
            bot.remove(instruction)
        }
    }
}

private fun onScrapInstructionExecuted(instruction: Instruction) {
    bot.add(
        createScrapInstruction(
            System.currentTimeMillis() + configurationRepository.getScrapPeriodMillis().getOrNull()!!
        )
    )
}

private fun onSendMessageInstructionExecuted(instruction: Instruction) {
    instruction as SendMessageInstruction
    configurationRepository.markMessageAsSent(instruction.message)
    try {
        bot.add(getSendInstruction())
    } catch (e: Exception) {
        logger.e(e)
        appScope.launch {
            delay(30000)
            onSendMessageInstructionExecuted(instruction)
        }
    } catch (e: Exception) {
        logger.e(e)
    }
}

private fun createLoginInstruction(authCode: String?, appState: ApplicationStore): LoginInstruction {
    return LoginInstruction(
        telegramRepository,
        configurationRepository,
        { onAuthRequested(appState) },
        authCode,
        LOGIN_INSTRUCTION_ID,
        0,
        "Login instruction",
        ::onLoginInstructionExecuted
    )
}

private fun onAuthRequested(appState: ApplicationStore) {
    appState.setExpectingAuthCode(true)
    bot.stop(true)
    bot.remove(0)
}

private fun onAuthEntered(authCode: String, appState: ApplicationStore) {
    bot.add(
        createLoginInstruction(authCode, appState)
    )
    bot.start()
}

private fun createScrapInstruction(executionTime: Long): ScrapGroupInstruction {
    return ScrapGroupInstruction(
        telegramRepository,
        configurationRepository,
        SCRAP_GROUP_INSTRUCTION_ID,
        executionTime,
        "Scrap group for users instructions",
        ::onScrapInstructionExecuted
    )
}

private fun getSendInstruction(): SendMessageInstruction {
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
        ::onSendMessageInstructionExecuted
    )
}
