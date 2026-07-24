package listing.ca.centris

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Centris.ca SSR list + detail HTML. See tmp/ca/api/centris/NOTES.md.
 */
class CentrisApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchListHtml(
        regionSlug: String,
        forRent: Boolean,
        page: Int,
        sortSeed: String?,
    ): String {
        val deal = if (forRent) "for-rent" else "for-sale"
        val base = "https://www.centris.ca/en/properties~$deal~$regionSlug"
        val url = buildString {
            append(base)
            if (page > 1 && !sortSeed.isNullOrBlank()) {
                append("?page=$page&sortSeed=$sortSeed")
            }
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-CA,en;q=0.9,fr-CA;q=0.8")
            header(HttpHeaders.Referrer, "https://www.centris.ca/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(pathOrUrl: String): String {
        val url = when {
            pathOrUrl.startsWith("http") -> pathOrUrl
            pathOrUrl.startsWith("/") -> "https://www.centris.ca$pathOrUrl"
            else -> "https://www.centris.ca/$pathOrUrl"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-CA,en;q=0.9,fr-CA;q=0.8")
            header(HttpHeaders.Referrer, "https://www.centris.ca/")
        }.bodyAsText()
        ensureHtml(text, requireCards = false)
        return text
    }

    private fun ensureHtml(text: String, requireCards: Boolean = true) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare"))
        // ListingPagingFail429 is embedded in page JS on every SERP — not a block signal.
        if (looksBlocked) {
            throw CentrisBlockedException("Centris.ca blocked or rate-limited")
        }
        if (requireCards && !text.contains("property-thumbnail-item")) {
            throw CentrisBlockedException("Centris.ca: missing list cards")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class CentrisBlockedException(message: String) : Exception(message)
