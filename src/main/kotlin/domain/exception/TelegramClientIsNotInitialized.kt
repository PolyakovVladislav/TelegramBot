package domain.exception

import data.dataSource.remote.TelegramRemoteDataSource.Companion.CLIENT_IS_NOT_INITIALIZED

class TelegramClientIsNotInitialized(message: String): TelegramException(CLIENT_IS_NOT_INITIALIZED, message)