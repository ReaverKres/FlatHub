package listing.gb.openrent

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * OpenRent GB — SSR HTML search + detail.
 * See tmp/gb/api/openrent/NOTES.md.
 */
class OpenRentApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(citySlug: String): String {
        val url = "https://www.openrent.co.uk/properties-to-rent/$citySlug"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.openrent.co.uk/")
        }.bodyAsText()
        ensureSearchHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(pathOrUrl: String): String {
        val url = when {
            pathOrUrl.startsWith("http") -> pathOrUrl
            pathOrUrl.startsWith("/") -> "https://www.openrent.co.uk$pathOrUrl"
            else -> "https://www.openrent.co.uk/$pathOrUrl"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-GB,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.openrent.co.uk/")
        }.bodyAsText()
        ensureDetailHtml(text)
        return text
    }

    private fun ensureSearchHtml(text: String) {
        val lower = text.lowercase()
        if (lower.contains("cf-mitigated") ||
            (lower.contains("just a moment") && lower.contains("cloudflare"))
        ) {
            throw OpenRentBlockedException("OpenRent WAF/challenge on search")
        }
        if (!text.contains("search-property-card") && !text.contains("PROPERTYIDS")) {
            throw OpenRentBlockedException("OpenRent: unexpected search HTML")
        }
    }

    private fun ensureDetailHtml(text: String) {
        val lower = text.lowercase()
        if (lower.contains("cf-mitigated") ||
            (lower.contains("just a moment") && lower.contains("cloudflare"))
        ) {
            throw OpenRentBlockedException("OpenRent WAF/challenge on detail")
        }
        if (!text.contains("property-to-rent") && !text.contains("data-lat")) {
            throw OpenRentBlockedException("OpenRent: unexpected detail HTML")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    }
}

class OpenRentBlockedException(message: String) : Exception(message)
