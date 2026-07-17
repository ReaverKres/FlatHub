package listing.ae.opensooq

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * OpenSooq UAE SSR HTML → `__NEXT_DATA__` serp listings.
 * See tmp/ae/api/opensooq/NOTES.md.
 */
class OpenSooqApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        citySlug: String,
        isSale: Boolean,
        isCommercial: Boolean,
        commercialKind: String? = null,
        page: Int,
    ): String {
        val kind = commercialKind ?: when {
            isCommercial && isSale -> "commercial-for-sale"
            isCommercial -> "commercial-for-rent"
            isSale -> "apartments-for-sale"
            else -> "apartments-for-rent"
        }
        val pageQs = if (page > 1) "?page=$page" else ""
        val url = "https://ae.opensooq.com/en/$citySlug/property/$kind$pageQs"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-AE,en;q=0.9,ar;q=0.8")
            header(HttpHeaders.Referrer, "https://ae.opensooq.com/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        val url = if (detailUrl.startsWith("http")) {
            detailUrl
        } else {
            "https://ae.opensooq.com${if (detailUrl.startsWith("/")) detailUrl else "/$detailUrl"}"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-AE,en;q=0.9,ar;q=0.8")
            header(HttpHeaders.Referrer, "https://ae.opensooq.com/")
        }.bodyAsText()
        ensureHtml(text, requireNextData = false)
        return text
    }

    private fun ensureHtml(text: String, requireNextData: Boolean = true) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare")) ||
                    lower.contains("pardon our interruption")
        if (looksBlocked) {
            throw OpenSooqBlockedException("OpenSooq captcha/WAF")
        }
        if (requireNextData && !text.contains("__NEXT_DATA__")) {
            throw OpenSooqBlockedException("OpenSooq: missing __NEXT_DATA__")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class OpenSooqBlockedException(message: String) : Exception(message)
