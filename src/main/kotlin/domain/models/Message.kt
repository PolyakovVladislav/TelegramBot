package domain.models

import java.util.Calendar
import java.util.Locale

class Message(
    val id: Long,
    val message: String,
    val time: String,
    val lastSentTimestamp: Long?
) {

    val hours = time.split(":").first().toInt()
    val minutes = time.split(":").last().toInt()

    val rtcTime: Long
        get() {
            val calendar = Calendar.getInstance(Locale.ENGLISH)
            if (calendar.get(Calendar.HOUR_OF_DAY) == hours &&
                calendar.get(Calendar.MINUTE) <= minutes) {
                calendar.set(Calendar.MINUTE, minutes)
            } else {
                while (calendar.get(Calendar.HOUR_OF_DAY) != hours) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                }
                calendar.set(Calendar.MINUTE, minutes)
            }
            return calendar.timeInMillis
        }
}
