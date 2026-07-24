package listing.ca.zolo

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Zolo.ca SSR gallery HTML. See tmp/ca/api/zolo/NOTES.md.
 */
class ZoloApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchGalleryHtml(
        slug: String,
        forRent: Boolean,
        page: Int,
    ): String {
        val deal = if (forRent) "for-rent" else "for-sale"
        val base = "https://www.zolo.ca/$slug-real-estate/$deal"
        val url = if (page > 1) "$base/page-$page" else base
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-CA,en;q=0.9")
            header(HttpHeaders.AcceptEncoding, "identity")
            header(HttpHeaders.Referrer, "https://www.zolo.ca/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(url: String): String {
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-CA,en;q=0.9")
            header(HttpHeaders.AcceptEncoding, "identity")
            header(HttpHeaders.Referrer, "https://www.zolo.ca/")
        }.bodyAsText()
        ensureHtml(text, requireCards = false)
        return text
    }

    private fun ensureHtml(text: String, requireCards: Boolean = true) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare")) ||
                    lower.contains("pardon our interruption") ||
                    (lower.contains("access denied") && !lower.contains("card-listing"))
        if (looksBlocked) {
            throw ZoloBlockedException("Zolo.ca Cloudflare/WAF")
        }
        if (requireCards && !text.contains("card-listing")) {
            throw ZoloBlockedException("Zolo.ca: missing card-listing")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class ZoloBlockedException(message: String) : Exception(message)
