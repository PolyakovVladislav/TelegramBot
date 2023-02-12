package domain.exception

import data.dataSource.remote.TelegramRemoteDataSource.Companion.TIMEOUT_CODE

class TelegramTimeoutException(message: String): TelegramException(TIMEOUT_CODE, message)