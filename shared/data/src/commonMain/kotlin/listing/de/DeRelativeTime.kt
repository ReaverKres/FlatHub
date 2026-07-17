package listing.de

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Parses DE listing timestamps: IS24 `vor 2 Tagen`, Kleinanzeigen `Heute, 17:15`.
 *
 * **Free-tier feed delay (default 60 min):** real “Heute, HH:mm” within the delay window
 * is intentionally hidden — same as other markets. Do **not** invent “now” timestamps
 * for scrapers without dates (they float to the top and then get stripped / confuse sort).
 */
object DeRelativeTime {
    fun parse(
        raw: String?,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Instant? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().replace('\u00A0', ' ')
        parseClockDate(s, now, timeZone)?.let { return it }
        return parseRelative(s.lowercase(), now)
    }

    /** `Heute, 17:15` / `Gestern, 09:05` / `15.07.2026` / `15.07.2026, 17:15`. */
    private fun parseClockDate(
        raw: String,
        now: Instant,
        timeZone: TimeZone,
    ): Instant? {
        val localNow = now.toLocalDateTime(timeZone)
        val heute = Regex(
            """^heute,?\s*(\d{1,2}):(\d{2})\s*$""",
            RegexOption.IGNORE_CASE,
        ).matchEntire(raw)
        if (heute != null) {
            val h = heute.groupValues[1].toInt()
            val m = heute.groupValues[2].toInt()
            var instant = localNow.date.atTime(LocalTime(h, m)).toInstant(timeZone)
            if (instant > now) instant -= 1.days
            return instant
        }
        val gestern = Regex(
            """^gestern,?\s*(\d{1,2}):(\d{2})\s*$""",
            RegexOption.IGNORE_CASE,
        ).matchEntire(raw)
        if (gestern != null) {
            val h = gestern.groupValues[1].toInt()
            val m = gestern.groupValues[2].toInt()
            return localNow.date.minus(1, DateTimeUnit.DAY)
                .atTime(LocalTime(h, m))
                .toInstant(timeZone)
        }
        val dotted = Regex(
            """^(\d{1,2})\.(\d{1,2})\.(\d{2,4})(?:,?\s*(\d{1,2}):(\d{2}))?\s*$""",
        ).matchEntire(raw)
        if (dotted != null) {
            val day = dotted.groupValues[1].toInt()
            val month = dotted.groupValues[2].toInt()
            var year = dotted.groupValues[3].toInt()
            if (year < 100) year += 2000
            val hour = dotted.groupValues[4].toIntOrNull() ?: 12
            val minute = dotted.groupValues[5].toIntOrNull() ?: 0
            return runCatching {
                LocalDate(year, month, day).atTime(LocalTime(hour, minute)).toInstant(timeZone)
            }.getOrNull()
        }
        return null
    }

    private fun parseRelative(s: String, now: Instant): Instant? {
        when {
            s == "gerade eben" || s == "soeben" || s.contains("sekunde") -> return now
            s == "gestern" -> return now - 1.days
            s.contains("minute") -> return now - numberOrOne(s).minutes
            s.contains("stunde") -> return now - numberOrOne(s).hours
            s.contains("tag") -> return now - numberOrOne(s).days
            s.contains("woche") -> return now - (numberOrOne(s) * 7).days
            s.contains("monat") -> return now - (numberOrOne(s) * 30).days
        }
        return null
    }

    private fun numberOrOne(s: String): Int {
        if (s.contains("einer") || s.contains("einem") || s.contains("eine ")) return 1
        return Regex("""(\d+)""").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }
}
