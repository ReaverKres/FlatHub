package listing.ch.flatfox

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import listing.ch.flatfox.FlatfoxApiClient.Companion.MAX_LIST_PAGE
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull

/**
 * Flatfox.ch anonymous REST — pin bbox + batched public-listing (`pk=` + `expand=images`).
 * See tmp/ch/api/flatfox/NOTES.md.
 */
class FlatfoxApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchPins(bbox: FlatfoxCities.Bbox): JsonArray? {
        val url =
            "$BASE/api/v1/pin/?north=${bbox.north}&south=${bbox.south}" +
                    "&east=${bbox.east}&west=${bbox.west}" +
                    "&object_category=APARTMENT&renting_type=RENT"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, "$BASE/en/search/")
        }.bodyAsText()
        ensureNotBlocked(text)
        return runCatching { json.parseToJsonElement(text).asArrayOrNull() }.getOrNull()
    }

    /**
     * Batch hydrate by listing pk. API hard-caps results at [MAX_LIST_PAGE] even if more pks sent.
     * `expand=images` returns signed thumb objects (not bare media ids).
     */
    suspend fun fetchListingsByPks(pks: List<Long>): List<JsonObject> {
        if (pks.isEmpty()) return emptyList()
        require(pks.size <= MAX_LIST_PAGE) {
            "Flatfox public-listing batch max $MAX_LIST_PAGE (got ${pks.size})"
        }
        val pkQuery = pks.joinToString("&") { "pk=$it" }
        val url =
            "$BASE/api/v1/public-listing/?limit=${pks.size}&offset=0" +
                    "&renting_type=RENT&expand=images&$pkQuery"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, "$BASE/en/search/")
        }.bodyAsText()
        ensureNotBlocked(text)
        val root = runCatching { json.parseToJsonElement(text).asObjectOrNull() }.getOrNull()
            ?: return emptyList()
        return root["results"].asArrayOrNull().orEmpty().mapNotNull { it.asObjectOrNull() }
    }

    suspend fun fetchListing(pk: Long): JsonObject? {
        val text = httpClient.get("$BASE/api/v1/public-listing/$pk/?expand=images") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, "$BASE/en/search/")
        }.bodyAsText()
        ensureNotBlocked(text)
        return runCatching { json.parseToJsonElement(text).asObjectOrNull() }.getOrNull()
    }

    private fun ensureNotBlocked(text: String) {
        val lower = text.lowercase()
        if (lower.contains("just a moment") || lower.contains("cf-mitigated")) {
            throw FlatfoxBlockedException("Flatfox captcha/WAF")
        }
        if (text.contains("403 Forbidden") &&
            !text.trimStart().startsWith("{") &&
            !text.trimStart().startsWith("[")
        ) {
            throw FlatfoxBlockedException("Flatfox 403")
        }
    }

    companion object {
        const val BASE = "https://flatfox.ch"

        /** Observed max `results` for `pk=` batch even when requesting 120. */
        const val MAX_LIST_PAGE = 100
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class FlatfoxBlockedException(message: String) : Exception(message)
