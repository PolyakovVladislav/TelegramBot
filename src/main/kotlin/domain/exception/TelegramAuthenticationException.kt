package domain.exception

import data.dataSource.remote.TelegramRemoteDataSource.Companion.AUTHORIZATION_ERROR

class TelegramAuthenticationException(message: String): TelegramException(AUTHORIZATION_ERROR, message)