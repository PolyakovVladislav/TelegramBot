package data.dataSource.remote

import LOGGER_LEVEL
import configurationRepository
import data.dataSource.TelegramDataSource
import data.dataSource.local.ConfigurationsLocalDataSource
import data.model.TelegramAuthenticationResult
import domain.exception.TelegramAuthenticationException
import domain.exception.TelegramClientIsNotInitialized
import domain.exception.TelegramException
import domain.exception.TelegramTimeoutException
import domain.exception.TooManyRequestsException
import it.tdlight.client.APIToken
import it.tdlight.client.AuthenticationData
import it.tdlight.client.SimpleTelegramClient
import it.tdlight.client.TDLibSettings
import it.tdlight.common.Init
import it.tdlight.jni.TdApi
import it.tdlight.jni.TdApi.AuthorizationStateClosed
import it.tdlight.jni.TdApi.AuthorizationStateClosing
import it.tdlight.jni.TdApi.AuthorizationStateLoggingOut
import it.tdlight.jni.TdApi.AuthorizationStateReady
import it.tdlight.jni.TdApi.AuthorizationStateWaitCode
import it.tdlight.jni.TdApi.AuthorizationStateWaitPhoneNumber
import it.tdlight.jni.TdApi.AuthorizationStateWaitTdlibParameters
import it.tdlight.jni.TdApi.BasicGroupFullInfo
import it.tdlight.jni.TdApi.Chat
import it.tdlight.jni.TdApi.ChatMembers
import it.tdlight.jni.TdApi.Chats
import it.tdlight.jni.TdApi.CheckAuthenticationCode
import it.tdlight.jni.TdApi.FormattedText
import it.tdlight.jni.TdApi.GetBasicGroupFullInfo
import it.tdlight.jni.TdApi.GetChat
import it.tdlight.jni.TdApi.GetChatHistory
import it.tdlight.jni.TdApi.GetSupergroupFullInfo
import it.tdlight.jni.TdApi.GetSupergroupMembers
import it.tdlight.jni.TdApi.InputMessageText
import it.tdlight.jni.TdApi.Message
import it.tdlight.jni.TdApi.Messages
import it.tdlight.jni.TdApi.SearchPublicChat
import it.tdlight.jni.TdApi.SearchPublicChats
import it.tdlight.jni.TdApi.SendMessage
import it.tdlight.jni.TdApi.SetTdlibParameters
import it.tdlight.jni.TdApi.SupergroupFullInfo
import it.tdlight.jni.TdApi.UpdateAuthorizationState
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import utils.Logger
import utils.Logger.Level.Release
import utils.Logger.Level.Staging
import utils.Strings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.path.Path

class TelegramRemoteDataSource : TelegramDataSource {

    companion object {
        const val GROUP_CANT_BE_SCRAPED = 0
        const val UNSUPPORTED_GROUP_TYPE = 1
        const val CHAT_NOT_FOUND = 2
        const val CLIENT_IS_NOT_INITIALIZED = 3
        const val TIMEOUT_CODE = 4
        const val AUTHORIZATION_ERROR = 5
        const val TOO_MANY_REQUESTS = 429
        const val TIMEOUT = 600_000L
    }

    private val log = Logger(this.javaClass.simpleName, configurationRepository, LOGGER_LEVEL)

    private var _client: SimpleTelegramClient? = null
    private val client: SimpleTelegramClient
        get() {
            if (_client != null) {
                return _client!!
            } else {
                throw TelegramClientIsNotInitialized("Client is not initialized")
            }
        }

    @Throws(TelegramTimeoutException::class, TelegramClientIsNotInitialized::class)
    override suspend fun login(phoneNumber: Long, apiId: Int, apiHash: String): TelegramAuthenticationResult {
        log("Attempt to login:", logLevel = Release)
        return executeWithTimeout(TIMEOUT) {
            return@executeWithTimeout suspendCancellableCoroutine {
                if (_client != null) {
                    _client!!.closeAndWait()
                    _client = null
                }
                Init.start()
                val apiToken = APIToken(apiId, apiHash)
                val settings = TDLibSettings.create(apiToken)
                val sessionPath = Path("${ConfigurationsLocalDataSource.CONFIG_DIRECTORY.path}\\tdlight")
                settings.databaseDirectoryPath = sessionPath.resolve("data")
                settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")
                _client = SimpleTelegramClient(settings)

                client.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
                    log("UpdateAuthorizationState: ${update.authorizationState}", logLevel = Release)
                    try {
                        val result = when (update.authorizationState) {
                            is AuthorizationStateReady -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.LOGGED_IN,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateClosing -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.CLOSING,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateClosed -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.CLOSED,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateLoggingOut -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.LOGGING_OUT,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateWaitCode -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.WAITING_FOR_AUTH_CODE,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateWaitTdlibParameters -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.WAITING_FOR_TDLIB_PARAMETERS,
                                    update.authorizationState.toString(),
                                )
                            }

                            is AuthorizationStateWaitPhoneNumber -> {
                                TelegramAuthenticationResult(
                                    TelegramAuthenticationResult.WAITING_FOR_PHONE_NUMBER,
                                    update.authorizationState.toString(),
                                )
                            }

                            else -> {
                                throw TelegramAuthenticationException("Unknown authorization state: \n$update")
                            }
                        }
                        log("Result code: ${result.code}", logLevel = Release)
                        when (result.code) {
                            TelegramAuthenticationResult.WAITING_FOR_TDLIB_PARAMETERS -> {
                                val parameters = SetTdlibParameters()
                                parameters.databaseDirectory = "tdlib"
                                parameters.useMessageDatabase = true
                                parameters.useSecretChats = true
                                parameters.apiId = apiId
                                parameters.apiHash = apiHash
                                parameters.systemLanguageCode = "en"
                                parameters.deviceModel = "Desktop"
                                parameters.applicationVersion = "1.0"
                                parameters.enableStorageOptimizer = true
                                client.send(parameters) { response ->
                                    if (response.isError) {
                                        it.resumeWithException(response.error.mapToTelegramException())
                                    }
                                }
                            }

                            TelegramAuthenticationResult.WAITING_FOR_AUTH_CODE -> {
                                it.resume(result)
                            }

                            TelegramAuthenticationResult.WAITING_FOR_PHONE_NUMBER -> {
                                it.resume(result)
                            }

                            TelegramAuthenticationResult.LOGGED_IN -> {
                                it.resume(result)
                            }

                            else -> {
                                throw TelegramAuthenticationException(
                                    "Unexpected authorization state:\n${result.state}",
                                )
                            }
                        }
                    } catch (e: TelegramAuthenticationException) {
                        it.resumeWithException(e)
                    }
                }

                val authenticationData = AuthenticationData.user(phoneNumber)
                client.start(authenticationData)
            }
        }
    }

    @Throws(TelegramTimeoutException::class, TelegramClientIsNotInitialized::class)
    override suspend fun confirmAuthorizationWithCode(authcode: String) {
        return executeWithTimeout(TIMEOUT) {
            log("confirmAuthorizationWithCode:", logLevel = Release)
            return@executeWithTimeout suspendCancellableCoroutine {
                client.send(CheckAuthenticationCode(authcode)) { result ->
                    log("result: ${result.get()}", logLevel = Release)
                    if (result.isError) {
                        it.resumeWithException(result.error.mapToTelegramException())
                    } else {
                        it.resume(Unit)
                    }
                }
            }
        }
    }

    override suspend fun searchPublicChat(groupLink: String): Chat {
        return executeWithTimeout(TIMEOUT) {
            log("searchPublicChats:", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(SearchPublicChat(groupLink)) { searchResult ->
                    log("searchResult: ${searchResult.get()}", logLevel = Staging)
                    if (searchResult.isError) {
                        continuation.resumeWithException(searchResult.error.mapToTelegramException())
                    } else {
                        continuation.resume(searchResult.get())
                    }
                }
            }
        }
    }

    override suspend fun searchPublicChats(groupLink: String): Chats {
        return executeWithTimeout(TIMEOUT) {
            log("searchPublicChats:", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(SearchPublicChats(groupLink)) { searchResult ->
                    log("searchResult: ${searchResult.get()}", logLevel = Staging)
                    if (searchResult.isError) {
                        continuation.resumeWithException(searchResult.error.mapToTelegramException())
                    } else if (searchResult.get().totalCount == 0) {
                        continuation.resumeWithException(
                            TelegramException(CHAT_NOT_FOUND, "Chat with name $groupLink not found"),
                        )
                    } else {
                        continuation.resume(searchResult.get())
                    }
                }
            }
        }
    }

    override suspend fun getChat(chatId: Long): Chat {
        return executeWithTimeout(TIMEOUT) {
            log("getChat:", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(GetChat(chatId)) { chatResult ->
                    log("chatResult: ${chatResult.get()}", logLevel = Staging)
                    if (chatResult.isError) {
                        continuation.resumeWithException(chatResult.error.mapToTelegramException())
                    } else {
                        continuation.resume(chatResult.get())
                    }
                }
            }
        }
    }

    override suspend fun getSuperGroupFullInfo(supergroupId: Long): SupergroupFullInfo {
        return executeWithTimeout(TIMEOUT) {
            log("getSuperGroupFullInfo:", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(GetSupergroupFullInfo(supergroupId)) { superGroupInfo ->
                    log("superGroupInfo: ${superGroupInfo.get()}", logLevel = Staging)
                    if (superGroupInfo.isError) {
                        continuation.resumeWithException(
                            superGroupInfo.error.mapToTelegramException(),
                        )
                    } else {
                        continuation.resume(superGroupInfo.get())
                    }
                }
            }
        }
    }

    override suspend fun getBasicGroupFullInfo(chatId: Long): BasicGroupFullInfo {
        return executeWithTimeout(TIMEOUT) {
            log("getBasicGroupFullInfo:", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(GetBasicGroupFullInfo(chatId)) { groupInfo ->
                    log("groupInfo: ${groupInfo.get()}", logLevel = Staging)
                    if (groupInfo.isError) {
                        continuation.resumeWithException(groupInfo.error.mapToTelegramException())
                    } else {
                        continuation.resume(groupInfo.get())
                    }
                }
            }
        }
    }

    @Throws(TelegramTimeoutException::class, TelegramClientIsNotInitialized::class, TelegramException::class)
    override suspend fun inviteUserToGroup(chatId: Long, users: LongArray) {
        return executeWithTimeout(TIMEOUT) {
            log("inviteUserToGroup", logLevel = Staging)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(TdApi.AddChatMembers(chatId, users)) { inviteResult ->
                    log("inviteResult: ${inviteResult.get()}", logLevel = Staging)
                    if (inviteResult.isError) {
                        continuation.resumeWithException(
                            inviteResult.error.mapToTelegramException(),
                        )
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    @Throws(TelegramTimeoutException::class, TelegramClientIsNotInitialized::class, TelegramException::class)
    override suspend fun sendMessageToGroup(chatId: Long, message: String) {
        return executeWithTimeout(TIMEOUT) {
            log("sendMessageToGroup:", logLevel = Staging)
            val inputMessageContent = InputMessageText(FormattedText(message, null), false, true)
            return@executeWithTimeout suspendCancellableCoroutine { continuation ->
                client.send(
                    SendMessage(
                        chatId,
                        0,
                        0,
                        null,
                        null,
                        inputMessageContent,
                    ),
                ) { messageResult ->
                    log("messageResult: ${messageResult.get()}", logLevel = Staging)
                    if (messageResult.isError) {
                        continuation.resumeWithException(
                            messageResult.error.mapToTelegramException(),
                        )
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    override suspend fun sendMessageToUser(userId: Long, message: String) {
        return executeWithTimeout(TIMEOUT) {
            log("sendMessageToUser: userId: $userId, message: $message", logLevel = Staging)
            suspendCancellableCoroutine { continuation ->
                val inputMessageContent = InputMessageText(FormattedText(message, null), false, true)
                client.send(
                    SendMessage(
                        userId,
                        0,
                        0,
                        null,
                        null,
                        inputMessageContent,
                    ),
                ) { messageResult ->
                    log("messageResult: ${messageResult.get()}", logLevel = Staging)
                    if (messageResult.isError) {
                        continuation.resumeWithException(
                            messageResult.error.mapToTelegramException(),
                        )
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    override suspend fun getSuperGroupMembers(supergroupId: Long, offset: Int, limit: Int): ChatMembers {
        return executeWithTimeout(TIMEOUT) {
            log("getSuperGroupMembers: supergroupId = $supergroupId, offset = $offset, limit = $limit", logLevel = Staging)
            suspendCancellableCoroutine { continuation ->
                client.send(
                    GetSupergroupMembers(
                        supergroupId,
                        null,
                        offset,
                        limit,
                    ),
                ) { membersResult ->
                    log("membersResult: ${membersResult.get()}", logLevel = Staging)
                    if (membersResult.isError) {
                        if (membersResult.error.code == TOO_MANY_REQUESTS) {
                            continuation.resumeWithException(
                                membersResult.error.mapToTelegramException(),
                            )
                        } else {
                            membersResult.error.mapToTelegramException()
                                .printStackTrace()
                        }
                    } else {
                        continuation.resume(membersResult.get())
                    }
                }
            }
        }
    }

    /**
     * To get messages history from newer to older set offset to negative value
     * Maximal possible API limit is 100
     */
    override suspend fun getMessagesHistory(chatId: Long, fromMessageId: Long, offset: Int, limit: Int): Messages {
        return executeWithTimeout(TIMEOUT) {
            log("getMessagesHistory: chatId = $chatId, offset = $offset, limit = $limit", logLevel = Staging)
            suspendCancellableCoroutine { continuation ->
                client.send(GetChatHistory(chatId, fromMessageId ,offset, limit, true))  {  result ->
                    if (result.isError) {
                        continuation.resumeWithException(result.error.mapToTelegramException())
                    } else {
                        continuation.resume(result.get())
                    }
                }
            }
        }
    }

    override suspend fun getRepliesForMessage(message: Message): Messages {
        return executeWithTimeout(TIMEOUT) {
            log("getRepliesForMessage: message = $message", logLevel = Staging)
            suspendCancellableCoroutine { continuation ->
                message.content
            }
        }
    }

    private fun TdApi.Error.mapToTelegramException(): TelegramException {
        return when (code) {
            TOO_MANY_REQUESTS -> {
                val retryTime = Strings.search(message, "Too Many Requests: retry after ", "\"").toLong()
                TooManyRequestsException(retryTime, message)
            }

            else -> {
                TelegramException(code, message)
            }
        }
    }

    private suspend fun <T> executeWithTimeout(timeout: Long, action: suspend () -> T): T {
        return try {
            withTimeout(timeout) {
                action()
            }
        } catch (e: TimeoutCancellationException) {
            throw TelegramTimeoutException("Timeout after $timeout ms")
        }
    }
}
