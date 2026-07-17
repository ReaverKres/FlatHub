package listing.ae.opensooq

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * OpenSooq SERP `posted_at` / `inserted_date`.
 *
 * Live shapes (en locale):
 * - relative: `16 minutes ago`, `4 hours ago`, `2 days ago`, `Yesterday`, `Today`
 * - absolute date-only: `15-07-2026`, `2026-07-15`
 *
 * Relative strings carry real clock time (approx); date-only has no time on the wire —
 * we keep local noon so UI still shows `HH:mm DD.MM.YYYY` via [DateConverter.formatInstant]
 * without clustering everything at midnight.
 */
object OpenSooqPostedAt {
    private val minutesAgo = Regex(
        """^(?:an?\s+)?(\d+)?\s*minutes?\s+ago$""",
        RegexOption.IGNORE_CASE,
    )
    private val hoursAgo = Regex(
        """^(?:an?\s+)?(\d+)?\s*hours?\s+ago$""",
        RegexOption.IGNORE_CASE,
    )
    private val daysAgo = Regex(
        """^(?:an?\s+)?(\d+)?\s*days?\s+ago$""",
        RegexOption.IGNORE_CASE,
    )
    private val weeksAgo = Regex(
        """^(?:an?\s+)?(\d+)?\s*weeks?\s+ago$""",
        RegexOption.IGNORE_CASE,
    )
    private val monthsAgo = Regex(
        """^(?:an?\s+)?(\d+)?\s*months?\s+ago$""",
        RegexOption.IGNORE_CASE,
    )
    private val absoluteDate = Regex(
        """^(\d{1,4})[-/.](\d{1,2})[-/.](\d{1,4})$""",
    )

    fun parse(
        raw: String?,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Instant? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().replace('\u00A0', ' ')
        parseRelative(s, now)?.let { return it }
        return parseAbsoluteDate(s, timeZone)
    }

    private fun parseRelative(raw: String, now: Instant): Instant? {
        val s = raw.lowercase()
        when {
            s == "just now" || s == "now" || s.contains("second") -> return now
            s == "yesterday" -> return now - 1.days
            s == "today" -> return now
        }
        minutesAgo.matchEntire(raw)?.let { m ->
            return now - (m.groupValues[1].toIntOrNull() ?: 1).minutes
        }
        hoursAgo.matchEntire(raw)?.let { m ->
            return now - (m.groupValues[1].toIntOrNull() ?: 1).hours
        }
        daysAgo.matchEntire(raw)?.let { m ->
            return now - (m.groupValues[1].toIntOrNull() ?: 1).days
        }
        weeksAgo.matchEntire(raw)?.let { m ->
            return now - ((m.groupValues[1].toIntOrNull() ?: 1) * 7).days
        }
        monthsAgo.matchEntire(raw)?.let { m ->
            return now - ((m.groupValues[1].toIntOrNull() ?: 1) * 30).days
        }
        return null
    }

    private fun parseAbsoluteDate(raw: String, timeZone: TimeZone): Instant? {
        val m = absoluteDate.matchEntire(raw) ?: return null
        val a = m.groupValues[1].toIntOrNull() ?: return null
        val b = m.groupValues[2].toIntOrNull() ?: return null
        val c = m.groupValues[3].toIntOrNull() ?: return null
        val (y, month, d) = when {
            // YYYY-MM-DD
            a >= 1000 -> Triple(a, b, c)
            // DD-MM-YYYY
            c >= 1000 -> Triple(c, b, a)
            else -> return null
        }
        return runCatching {
            // No clock on the wire — noon local avoids 00:00 UI and midnight sort pile-up.
            LocalDate(y, month, d).atTime(LocalTime(12, 0)).toInstant(timeZone)
        }.getOrNull()
    }
}
