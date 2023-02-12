package utils

import domain.repositories.ConfigurationsRepository
import java.text.SimpleDateFormat
import java.util.*

class Logger(
    private val classname: String,
    private val configs: ConfigurationsRepository,
    private val logLevel: Level = Level.Debug
) {

    operator fun invoke(message: String, tag: String = "", logLevel: Level = Level.Debug) {
        if (logLevel.level >= this.logLevel.level) {
            var internalTag = tag
            if (tag != "") {
                internalTag += " "
            }
            val date = SimpleDateFormat("HH:mm:ss.SSS").format(
                Date(
                    System.currentTimeMillis()
                )
            )
            val row = "$classname [$date]: $tag $message"
            println(row)
            configs.log(listOf(row))
        }
    }

    fun e(e: Throwable, tag: String = "", logLevel: Level = Level.Debug) {
        if (logLevel.level >= this.logLevel.level) {
            var internalTag = tag
            if (tag != "") {
                internalTag += " "
            }
            val date = SimpleDateFormat("HH:mm:ss.SSS").format(
                Date(
                    System.currentTimeMillis()
                )
            )
            val row = "$classname [$date]: $tag ${e.javaClass}: ${e.message}.\n" +
                "Stacktrace:\n     ${e.stackTrace.joinToString("\n     ")}"
            println(row)
            configs.log(listOf(row))
        }
    }

    enum class Level(val level: Int) {
        Debug(0),
        Staging(1),
        Release(2)
    }
}
