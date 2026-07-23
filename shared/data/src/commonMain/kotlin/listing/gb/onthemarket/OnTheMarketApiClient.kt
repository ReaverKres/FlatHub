package listing.gb.onthemarket

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * OnTheMarket SSR HTML with inline __NEXT_DATA__ (initialReduxState).
 * Do not call _next/data JSON routes (403). See tmp/gb/api/onthemarket/NOTES.md.
 */
class OnTheMarketApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        locationSlug: String,
        isSale: Boolean,
        page: Int,
    ): String {
        val segment = if (isSale) "for-sale" else "to-rent"
        val url = "$BASE/$segment/property/$locationSlug/" +
                if (page > 1) "?page=$page" else ""
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, BASE)
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(adId: Long): String {
        val text = httpClient.get("$BASE/details/$adId/") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, BASE)
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    private fun ensureHtml(text: String) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare")) ||
                    (lower.contains("captcha") && !text.contains("__NEXT_DATA__"))
        if (looksBlocked && !text.contains("__NEXT_DATA__")) {
            throw OnTheMarketBlockedException("OnTheMarket captcha/WAF")
        }
        if (!text.contains("__NEXT_DATA__")) {
            throw OnTheMarketBlockedException("OnTheMarket: missing __NEXT_DATA__")
        }
    }

    companion object {
        private const val BASE = "https://www.onthemarket.com"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class OnTheMarketBlockedException(message: String) : Exception(message)
