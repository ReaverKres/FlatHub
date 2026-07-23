package listing.gb

import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.longOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Shared GB publishedAt helpers. Only maps real site signals — never invents "now"
 * when the portal has no date field at all.
 *
 * Rightmove: nested `listingUpdate.listingUpdateDate`, ISO strings, epoch millis.
 * OnTheMarket: `days-since-added-reduced` / labels like "Reduced today".
 * OpenRent: "Last updated around 1 week ago" on SERP cards.
 */
object GbPublishedAt {
    data class TripleDates(
        val publishedAt: Instant?,
        val publishedAtServer: String?,
        val publishedAtUi: String?,
    )

    fun fromRightmove(obj: JsonObject): TripleDates? {
        val candidates = buildList {
            obj["listingUpdate"].asObjectOrNull()?.let { lu ->
                add(lu["listingUpdateDate"])
                add(lu["listingUpdateDateTime"])
            }
            add(obj["firstVisibleDate"])
            add(obj["listingUpdateDate"])
            add(obj["addedOn"])
            add(obj["publishedOn"])
            add(obj["updateDate"])
            add(obj["addedOrReduced"])
        }
        for (el in candidates) {
            parseWire(el)?.let { return it }
        }
        return null
    }

    fun fromOnTheMarket(obj: JsonObject, now: Instant = Clock.System.now()): TripleDates? {
        val daysEl = obj["days-since-added-reduced"] ?: obj["daysSinceAddedReduced"]
        val days = daysEl.longOrNull()
            ?: daysEl.doubleOrNull()?.toLong()
            ?: daysEl.contentOrNull()?.toLongOrNull()
        if (days != null && days >= 0) {
            val instant = now - days.days
            return pack(instant, days.toString())
        }
        val label = obj["main-label"].contentOrNull()
            ?: obj["data-label-id"].contentOrNull()
        parseRelativeLabel(label, now)?.let { return pack(it, label) }
        return null
    }

    fun fromOpenRentRelative(raw: String?, now: Instant = Clock.System.now()): TripleDates? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().lowercase().replace('\u00A0', ' ')
        val instant = when {
            s.contains("just now") || s.contains("minute") -> now
            Regex("""(\d+)\s*hours?\s+ago""").find(s) != null -> {
                val h =
                    Regex("""(\d+)\s*hours?\s+ago""").find(s)!!.groupValues[1].toLongOrNull() ?: 1
                now - h.hours
            }

            Regex("""(\d+)\s*days?\s+ago""").find(s) != null -> {
                val d =
                    Regex("""(\d+)\s*days?\s+ago""").find(s)!!.groupValues[1].toLongOrNull() ?: 1
                now - d.days
            }

            Regex("""(\d+)\s*weeks?\s+ago""").find(s) != null -> {
                val w =
                    Regex("""(\d+)\s*weeks?\s+ago""").find(s)!!.groupValues[1].toLongOrNull() ?: 1
                now - (w * 7).days
            }

            Regex("""(\d+)\s*months?\s+ago""").find(s) != null -> {
                val mo =
                    Regex("""(\d+)\s*months?\s+ago""").find(s)!!.groupValues[1].toLongOrNull() ?: 1
                now - (mo * 30).days
            }

            s.contains("yesterday") -> now - 1.days
            s.contains("today") -> now
            else -> return null
        }
        return pack(instant, raw.trim())
    }

    private fun pack(instant: Instant, server: String?): TripleDates = TripleDates(
        publishedAt = instant,
        publishedAtServer = server,
        publishedAtUi = DateConverter.formatInstant(
            instant,
            TimeZone.currentSystemDefault(),
        ),
    )

    private fun parseWire(el: JsonElement?): TripleDates? {
        if (el == null) return null
        el.longOrNull()?.let { epoch ->
            val millis = if (epoch < 1_000_000_000_000L) epoch * 1000 else epoch
            if (millis < 946_684_800_000L) return null // before 2000-01-01
            return pack(Instant.fromEpochMilliseconds(millis), epoch.toString())
        }
        val raw = el.contentOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        parseIsoOrUkDate(raw)?.let { return pack(it, raw) }
        return null
    }

    private fun parseIsoOrUkDate(raw: String): Instant? {
        val s = raw.trim()
        if (s.contains('T') || s.matches(Regex("""\d{4}-\d{2}-\d{2}.*"""))) {
            runCatching { Instant.parse(s) }.getOrNull()?.let { return it }
            val d = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(s)
            if (d != null) {
                return runCatching {
                    Instant.parse("${d.groupValues[1]}-${d.groupValues[2]}-${d.groupValues[3]}T12:00:00Z")
                }.getOrNull()
            }
        }
        val uk = Regex(
            """(?:added|reduced)\s+on\s+(\d{1,2})/(\d{1,2})/(\d{4})|(\d{1,2})/(\d{1,2})/(\d{4})""",
            RegexOption.IGNORE_CASE,
        ).find(s) ?: return null
        val day = (uk.groupValues[1].ifBlank { uk.groupValues[4] }).toInt()
        val month = (uk.groupValues[2].ifBlank { uk.groupValues[5] }).toInt()
        val year = (uk.groupValues[3].ifBlank { uk.groupValues[6] }).toInt()
        val y = year.toString().padStart(4, '0')
        val mo = month.toString().padStart(2, '0')
        val da = day.toString().padStart(2, '0')
        return runCatching { Instant.parse("${y}-${mo}-${da}T12:00:00Z") }.getOrNull()
    }

    private fun parseRelativeLabel(raw: String?, now: Instant): Instant? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().lowercase()
        return when {
            s.contains("today") -> now
            s.contains("yesterday") -> now - 1.days
            else -> {
                val m = Regex("""(\d+)\s*days?\s*(?:ago|since)""").find(s)
                m?.groupValues?.get(1)?.toLongOrNull()?.let { now - it.days }
            }
        }
    }
}
