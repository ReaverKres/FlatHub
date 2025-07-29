package io.flatzen.commoncomponents.date

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Утилитарный класс для преобразования дат в формате ISO 8601.
 */
object DateConverter {

    fun stringToInstant(dateString: String): Instant {
        return Instant.parse(dateString)
    }

    fun instantToString(instant: Instant): String {
        return instant.toString()
    }

    fun formatInstant(instant: Instant, timeZone: TimeZone): String {
        val localDateTime: LocalDateTime = instant.toLocalDateTime(timeZone)

        fun Int.pad(): String = this.toString().padStart(2, '0')

        val hour = localDateTime.hour.pad()
        val minute = localDateTime.minute.pad()
        val day = localDateTime.dayOfMonth.pad()
        val month = localDateTime.monthNumber.pad()
        val year = localDateTime.year

        return "$hour:$minute $day.$month.$year"
    }
}

