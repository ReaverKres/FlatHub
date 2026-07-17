package listing.ae.propertyfinder

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Property Finder SSR HTML → `__NEXT_DATA__` searchResult.
 * See tmp/ae/api/propertyfinder/NOTES.md.
 */
class PropertyFinderApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        locationId: Int,
        isSale: Boolean,
        isCommercial: Boolean,
        commercialTypeId: Int? = null,
        page: Int,
    ): String {
        // Residential: c=1 sale / c=2 rent (+ t=1 apartment).
        // Commercial: c=3 buy / c=4 rent; optional t= subtype.
        val c = when {
            isCommercial && isSale -> 3
            isCommercial -> 4
            isSale -> 1
            else -> 2
        }
        val typeQs = when {
            isCommercial && commercialTypeId != null -> "&t=$commercialTypeId"
            isCommercial -> ""
            else -> "&t=1"
        }
        val url =
            "https://www.propertyfinder.ae/en/search?c=$c$typeQs&l=$locationId&ob=nd&page=$page"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-AE,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.propertyfinder.ae/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        val url = if (detailUrl.startsWith("http")) {
            detailUrl
        } else {
            "https://www.propertyfinder.ae${if (detailUrl.startsWith("/")) detailUrl else "/$detailUrl"}"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-AE,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.propertyfinder.ae/")
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
            throw PropertyFinderBlockedException("Property Finder captcha/WAF")
        }
        if (!text.contains("__NEXT_DATA__")) {
            throw PropertyFinderBlockedException("Property Finder: missing __NEXT_DATA__")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class PropertyFinderBlockedException(message: String) : Exception(message)
