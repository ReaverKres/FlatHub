package listing.de.is24

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull

/**
 * ImmobilienScout24 mobile JSON API (not www HTML — AWS WAF).
 * See tmp/de/api/is24/NOTES.md.
 */
class Is24ApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun search(
        geocode: String,
        isSale: Boolean,
        pageNumber: Int,
        pageSize: Int,
        priceFrom: Int? = null,
        priceTo: Int? = null,
        roomsFrom: Int? = null,
        roomsTo: Int? = null,
    ): JsonObject {
        val text = httpClient.post("$BASE/search/list") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            contentType(ContentType.Application.Json)
            parameter("searchType", "region")
            parameter("realestatetype", if (isSale) "apartmentbuy" else "apartmentrent")
            // Sale: omit pricetype (purchaseprice → 412). Rent: warm rent total.
            if (!isSale) parameter("pricetype", "calculatedtotalrent")
            parameter("geocodes", geocode)
            parameter("pagenumber", pageNumber)
            parameter("pagesize", pageSize)
            if (priceFrom != null || priceTo != null) {
                val from = priceFrom ?: 0
                val to = priceTo ?: 99_999_999
                parameter("price", "$from-$to")
            }
            if (roomsFrom != null || roomsTo != null) {
                val from = roomsFrom ?: 0
                val to = roomsTo ?: 99
                parameter("numberofrooms", "$from-$to")
            }
            parameter("exclusioncriteria", "swapflat")
            // Default mobile sort is OnTop/relevance (`standard`) → days-old first.
            // Newest-first so IS24 interleaves with dated KA in FlatSort.NEWEST_FIRST.
            parameter("sorting", "-firstactivation")
            setBody(SEARCH_BODY)
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: error("IS24 search: not a JSON object")
    }

    suspend fun detail(exposeId: String): JsonObject {
        val text = httpClient.get("$BASE/expose/$exposeId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: error("IS24 detail: not a JSON object")
    }

    private fun ensureJson(text: String) {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<") ||
            trimmed.contains("Ich bin kein Roboter", ignoreCase = true) ||
            trimmed.contains("challenge.js", ignoreCase = true)
        ) {
            throw Is24BlockedException("IS24 returned HTML/WAF instead of JSON")
        }
        if (trimmed.contains("\"error\"") && trimmed.contains("ERROR_COMMON")) {
            throw Is24BlockedException("IS24 API error: ${trimmed.take(200)}")
        }
    }

    companion object {
        private const val BASE = "https://api.mobile.immobilienscout24.de"
        private const val USER_AGENT = "ImmoScout_27.12_26.2_._"
        private const val SEARCH_BODY =
            """{"supportedResultListTypes":[],"userData":{}}"""
    }
}

class Is24BlockedException(message: String) : Exception(message)
