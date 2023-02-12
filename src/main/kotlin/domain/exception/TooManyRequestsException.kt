package domain.exception

import data.dataSource.remote.TelegramRemoteDataSource.Companion.TOO_MANY_REQUESTS

class TooManyRequestsException(
    val retryAfterMillis: Long,
    message: String
): TelegramException(
    TOO_MANY_REQUESTS,
    message
)