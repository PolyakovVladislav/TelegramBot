// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.ConfigurationsRepositoryImpl
import data.TelegramRepositoryImpl
import data.dataSource.local.ConfigurationsLocalDataSource
import data.dataSource.remote.TelegramRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import utils.CloseableCoroutineScope
import utils.Logger

const val LOGIN_INSTRUCTION_ID = 0
const val SCRAP_GROUP_INSTRUCTION_ID = 1
const val SEND_MESSAGE_INSTRUCTION_ID = 2

val LOGGER_LEVEL = Logger.Level.Debug

private val configurationsDataSource = ConfigurationsLocalDataSource()
val configurationRepository = ConfigurationsRepositoryImpl(configurationsDataSource)

private val telegramDataSource = TelegramRemoteDataSource()
val telegramRepository = TelegramRepositoryImpl(telegramDataSource, configurationRepository)

val appScope = CloseableCoroutineScope(SupervisorJob() + Dispatchers.IO)

private val logger = Logger("Main", configurationRepository, LOGGER_LEVEL)

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Telegram Bot"
        ) {
            App()
        }
    }
}

