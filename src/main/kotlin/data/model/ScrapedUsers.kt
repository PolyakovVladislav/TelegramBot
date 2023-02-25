package data.model

import java.lang.Exception

sealed class ScrapedUsers {

    data class Result(
        val usersIds: List<Long>,
        val exception: Exception?
    ): ScrapedUsers()

    data class Progress(
        val scrappedCount: Int,
        val totalCount: Int
    ): ScrapedUsers() {

        val progress: Int
            get() = scrappedCount / totalCount * 100
    }
}