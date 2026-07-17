package listing.ae.dubizzle

import io.ktor.client.HttpClient
import io.ktor.client.request.header
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
 * Dubizzle Dubai residential search via public Algolia (site HTML is Incapsula-blocked).
 * See tmp/ae/api/dubizzle/NOTES.md.
 */
class DubizzleApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun search(
        cityId: Int,
        isSale: Boolean,
        page: Int,
        hitsPerPage: Int,
    ): JsonObject {
        val index = if (isSale) INDEX_SALE else INDEX_RENT
        val categoryId = if (isSale) CATEGORY_SALE_APARTMENT else CATEGORY_RENT_APARTMENT
        // Algolia pages are 0-based.
        val algoliaPage = (page - 1).coerceAtLeast(0)
        val body =
            """{"query":"","hitsPerPage":$hitsPerPage,"page":$algoliaPage,"filters":"(city.id=$cityId) AND (categories.ids=$categoryId)"}"""
        val text = httpClient.post("$ALGOLIA_HOST/1/indexes/$index/query") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header("X-Algolia-Application-Id", APP_ID)
            header("X-Algolia-API-Key", API_KEY)
            header(HttpHeaders.AcceptLanguage, "en-AE,en;q=0.9")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: error("Dubizzle Algolia: not a JSON object")
    }

    private fun ensureJson(text: String) {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<") ||
            trimmed.contains("Pardon Our Interruption", ignoreCase = true) ||
            trimmed.contains("captcha", ignoreCase = true)
        ) {
            throw DubizzleBlockedException("Dubizzle returned HTML/WAF instead of JSON")
        }
        if (trimmed.contains("\"message\":\"Invalid Application-ID") ||
            trimmed.contains("\"status\":403")
        ) {
            throw DubizzleBlockedException("Dubizzle Algolia key rejected: ${trimmed.take(200)}")
        }
    }

    companion object {
        private const val ALGOLIA_HOST = "https://WD0PTZ13ZS-dsn.algolia.net"
        private const val APP_ID = "WD0PTZ13ZS"
        private const val API_KEY = "cef139620248f1bc328a00fddc7107a6"
        private const val INDEX_RENT = "property-for-rent-residential.com"
        private const val INDEX_SALE = "property-for-sale-residential.com"

        /** Rent apartmentflat */
        private const val CATEGORY_RENT_APARTMENT = 24

        /** Sale apartment (observed live) */
        private const val CATEGORY_SALE_APARTMENT = 1742
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}

class DubizzleBlockedException(message: String) : Exception(message)
