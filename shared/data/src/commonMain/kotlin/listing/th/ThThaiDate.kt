package listing.th

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
 * Thai listing timestamps: relative bumps (`ดัน 3 วันที่แล้ว`) and Buddhist-era calendar dates.
 * Buddhist year = CE + 543 (e.g. 2569 → 2026).
 */
object ThThaiDate {
    private val relativeRe =
        Regex(
            """(?:ดัน\s*)?(\d+)\s*(นาที|ชั่วโมง|วัน|สัปดาห์|เดือน)(?:ที่แล้ว)?""",
        )
    private val slashDateRe =
        Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""")
    private val thaiMonthDateRe =
        Regex(
            """(\d{1,2})\s+(ม\.ค\.|ก\.พ\.|มี\.ค\.|เม\.ย\.|พ\.ค\.|มิ\.ย\.|ก\.ค\.|ส\.ค\.|ก\.ย\.|ต\.ค\.|พ\.ย\.|ธ\.ค\.)\s+(\d{4})""",
        )

    private val thaiMonths = mapOf(
        "ม.ค." to 1,
        "ก.พ." to 2,
        "มี.ค." to 3,
        "เม.ย." to 4,
        "พ.ค." to 5,
        "มิ.ย." to 6,
        "ก.ค." to 7,
        "ส.ค." to 8,
        "ก.ย." to 9,
        "ต.ค." to 10,
        "พ.ย." to 11,
        "ธ.ค." to 12,
    )

    fun parse(
        raw: String?,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Instant? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().replace('\u00A0', ' ')
        parseRelative(s, now)?.let { return it }
        parseSlashDate(s, timeZone)?.let { return it }
        parseThaiMonthDate(s, timeZone)?.let { return it }
        return null
    }

    private fun parseRelative(s: String, now: Instant): Instant? {
        val m = relativeRe.find(s) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return when (m.groupValues[2]) {
            "นาที" -> now - n.minutes
            "ชั่วโมง" -> now - n.hours
            "วัน" -> now - n.days
            "สัปดาห์" -> now - (n * 7).days
            "เดือน" -> now - (n * 30).days
            else -> null
        }
    }

    private fun parseSlashDate(s: String, timeZone: TimeZone): Instant? {
        val m = slashDateRe.find(s) ?: return null
        val day = m.groupValues[1].toInt()
        val month = m.groupValues[2].toInt()
        val year = toGregorianYear(m.groupValues[3].toInt())
        return runCatching {
            LocalDate(year, month, day).atTime(LocalTime(12, 0)).toInstant(timeZone)
        }.getOrNull()
    }

    private fun parseThaiMonthDate(s: String, timeZone: TimeZone): Instant? {
        val m = thaiMonthDateRe.find(s) ?: return null
        val day = m.groupValues[1].toInt()
        val month = thaiMonths[m.groupValues[2]] ?: return null
        val year = toGregorianYear(m.groupValues[3].toInt())
        return runCatching {
            LocalDate(year, month, day).atTime(LocalTime(12, 0)).toInstant(timeZone)
        }.getOrNull()
    }

    private fun toGregorianYear(year: Int): Int =
        if (year >= 2400) year - 543 else year
}
