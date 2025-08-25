package io.flatzen.commoncomponents.date

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Утилитарный класс для преобразования дат в формате ISO 8601.
 */
object DateConverter {

    fun parseDomovitaDate(dateString: String): Instant {

        // Пытаемся парсить как LocalDateTime (с временем) - "2025-08-26 03:02:05"
        return if (dateString.contains(" ")) {
            val parts = dateString.split(" ")
            val datePart = parts[0]
            val timePart = parts[1]

            val dateComponents = datePart.split("-").map { it.toInt() }
            val timeComponents = timePart.split(":").map { it.toInt() }

            val localDateTime = LocalDateTime(
                year = dateComponents[0],
                monthNumber = dateComponents[1],
                dayOfMonth = dateComponents[2],
                hour = timeComponents[0],
                minute = timeComponents[1],
                second = timeComponents[2],
                nanosecond = 0
            )
            localDateTime.toInstant(TimeZone.UTC)
        } else {
            // Парсим как LocalDate (без времени) - "2025-08-26"
            val dateComponents = dateString.split("-").map { it.toInt() }
            val localDate = LocalDate(
                year = dateComponents[0],
                monthNumber = dateComponents[1],
                dayOfMonth = dateComponents[2]
            )
            localDate.atTime(hour = 0, minute = 0).toInstant(TimeZone.UTC)
        }
    }

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

    fun formatInstantWithDomovitaDates(
        dateReception: Instant?,
        dateRevision: Instant,
        timeZone: TimeZone
    ): String {
        val baseDate = formatInstant(dateRevision, timeZone)

        if (dateReception == null) {
            return baseDate
        }

        // Проверяем, совпадают ли день, месяц и год
        val receptionLocal = dateReception.toLocalDateTime(timeZone)
        val revisionLocal = dateRevision.toLocalDateTime(timeZone)

        val datesMatch = receptionLocal.dayOfMonth == revisionLocal.dayOfMonth &&
                receptionLocal.month == revisionLocal.month &&
                receptionLocal.year == revisionLocal.year

        if (datesMatch) {
            fun Int.pad(): String = this.toString().padStart(2, '0')
            val hour = receptionLocal.hour.pad()
            val minute = receptionLocal.minute.pad()
            return "$hour:$minute $baseDate"
        }

        return baseDate
    }
}

