package listing.gb.rightmove

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull

/**
 * Rightmove GB — JSON `/api/_search` primary, `find.html` + `__NEXT_DATA__` fallback.
 * Detail: `/properties/{id}` → `PAGE_MODEL`. See tmp/gb/api/rightmove/NOTES.md.
 */
class RightmoveApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun searchJson(
        locationIdentifier: String,
        isSale: Boolean,
        index: Int,
        pageSize: Int,
        priceFrom: Int? = null,
        priceTo: Int? = null,
    ): JsonObject {
        val text = httpClient.get("$BASE/api/_search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/")
            parameter("locationIdentifier", locationIdentifier)
            parameter("channel", if (isSale) "BUY" else "RENT")
            parameter("index", index)
            parameter("numberOfPropertiesPerPage", pageSize)
            parameter("sortType", 6)
            parameter("radius", 0.0)
            parameter("viewType", "LIST")
            parameter("includeSSTC", false)
            parameter("areaSizeUnit", "sqft")
            parameter("currencyCode", "GBP")
            parameter("isFetching", false)
            priceFrom?.let { parameter("minPrice", it) }
            priceTo?.let { parameter("maxPrice", it) }
        }.bodyAsText()
        ensureSearchJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: throw RightmoveBlockedException("Rightmove search: not a JSON object")
    }

    suspend fun searchHtmlFallback(
        locationIdentifier: String,
        isSale: Boolean,
        index: Int,
    ): String {
        val path = if (isSale) {
            "/property-for-sale/find.html"
        } else {
            "/property-to-rent/find.html"
        }
        val text = httpClient.get("$BASE$path") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/")
            parameter("locationIdentifier", locationIdentifier)
            parameter("index", index)
        }.bodyAsText()
        ensureSearchHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(propertyId: Long): String {
        val text = httpClient.get("$BASE/properties/$propertyId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/")
        }.bodyAsText()
        ensureDetailHtml(text)
        return text
    }

    private fun ensureSearchJson(text: String) {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<") || trimmed.contains("__NEXT_DATA__")) {
            throw RightmoveBlockedException("Rightmove search API returned HTML")
        }
        if (!trimmed.startsWith("{")) {
            throw RightmoveBlockedException("Rightmove search: unexpected response")
        }
    }

    private fun ensureSearchHtml(text: String) {
        if (text.contains("Access Denied", ignoreCase = true)) {
            throw RightmoveBlockedException("Rightmove search HTML blocked")
        }
        if (!text.contains("__NEXT_DATA__")) {
            throw RightmoveBlockedException("Rightmove search: missing __NEXT_DATA__")
        }
    }

    private fun ensureDetailHtml(text: String) {
        if (text.contains("Access Denied", ignoreCase = true)) {
            throw RightmoveBlockedException("Rightmove detail blocked")
        }
        if (!text.contains("PAGE_MODEL")) {
            throw RightmoveBlockedException("Rightmove detail: missing PAGE_MODEL")
        }
    }

    companion object {
        private const val BASE = "https://www.rightmove.co.uk"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class RightmoveBlockedException(message: String) : Exception(message)
