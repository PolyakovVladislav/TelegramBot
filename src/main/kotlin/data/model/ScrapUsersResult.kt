package data.model

import java.lang.Exception

data class ScrapUsersResult(
    val usersIds: List<Long>,
    val exception: Exception?
)
