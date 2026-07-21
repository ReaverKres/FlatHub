package listing.at.willhaben

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * willhaben.at SSR HTML → `__NEXT_DATA__`. Soft-fail on IP block / missing payload.
 * See tmp/at/api/willhaben/NOTES.md.
 */
class WillhabenApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        areaId: Int,
        isSale: Boolean,
        page: Int,
        rows: Int = 30,
    ): String {
        val path = if (isSale) {
            "/iad/immobilien/eigentumswohnung/eigentumswohnung-angebote"
        } else {
            "/iad/immobilien/mietwohnungen/mietwohnung-angebote"
        }
        val text = httpClient.get("$BASE$path") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-AT,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "$BASE/iad/immobilien/")
            parameter("areaId", areaId)
            parameter("page", page)
            parameter("rows", rows)
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(adId: Long): String {
        val text = httpClient.get("$BASE/iad/object") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-AT,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "$BASE/iad/immobilien/")
            parameter("adId", adId)
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    private fun ensureHtml(text: String) {
        if (text.contains("IP-Adresse wurde blockiert", ignoreCase = true) ||
            text.contains("Your IP address is blocked", ignoreCase = true)
        ) {
            throw WillhabenBlockedException("willhaben IP block")
        }
        if (!text.contains("__NEXT_DATA__")) {
            throw WillhabenBlockedException("willhaben: missing __NEXT_DATA__")
        }
    }

    companion object {
        private const val BASE = "https://www.willhaben.at"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class WillhabenBlockedException(message: String) : Exception(message)
