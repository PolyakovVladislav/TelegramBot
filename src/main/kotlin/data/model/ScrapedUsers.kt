package data.model

sealed class ScrapedUsers {

    data class Result(
        val usersIds: List<Long>
    ): ScrapedUsers()

    data class Progress(
        val scrappedCount: Int,
        val totalCount: Int
    ): ScrapedUsers() {

        val progress: Int
            get() = scrappedCount / totalCount * 100
    }
}