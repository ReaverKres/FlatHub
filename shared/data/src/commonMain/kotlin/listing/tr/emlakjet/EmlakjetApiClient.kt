package listing.tr.emlakjet

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Emlakjet.com SSR HTML list + detail. See tmp/tr/api/emlakjet/NOTES.md.
 */
class EmlakjetApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        citySlug: String,
        isSale: Boolean,
        page: Int,
    ): String {
        val op = if (isSale) "satilik-konut" else "kiralik-konut"
        // No trailing slash — site 301s `/ankara/` → `/ankara`.
        val pageQs = if (page > 1) "?sayfa=$page" else ""
        val url = "https://www.emlakjet.com/$op/$citySlug$pageQs"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "tr-TR,tr;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.emlakjet.com/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        val url = if (detailUrl.startsWith("http")) {
            detailUrl
        } else {
            "https://www.emlakjet.com${if (detailUrl.startsWith("/")) detailUrl else "/$detailUrl"}"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "tr-TR,tr;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.emlakjet.com/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    /**
     * Real interstitials only. Do **not** match `/cdn-cgi/challenge-platform/scripts/jsd/` —
     * that bot-mgmt script is injected on every successful Emlakjet SSR page.
     */
    private fun ensureHtml(text: String) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("geo.captcha-delivery.com") ||
                    lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare"))
        if (looksBlocked && !text.contains("data-listing-id=")) {
            throw EmlakjetBlockedException("Emlakjet captcha/WAF")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class EmlakjetBlockedException(message: String) : Exception(message)
