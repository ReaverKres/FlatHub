package listing.de.kleinanzeigen

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Kleinanzeigen.de SSR HTML (former eBay Kleinanzeigen). Soft-fail on challenge pages.
 */
class KleinanzeigenApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        city: KleinanzeigenCities.CityLoc,
        isSale: Boolean,
        page: Int,
    ): String {
        val cat = if (isSale) "c196" else "c203"
        val catName = if (isSale) "s-wohnung-kaufen" else "s-wohnung-mieten"
        val pagePart = if (page > 1) "seite:$page/" else ""
        val url =
            "https://www.kleinanzeigen.de/$catName/${city.slug}/$pagePart$cat" +
                    "l${city.locationId}"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-DE,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.kleinanzeigen.de/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        val url = if (detailUrl.startsWith("http")) {
            detailUrl
        } else {
            "https://www.kleinanzeigen.de$detailUrl"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-DE,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.kleinanzeigen.de/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    private fun ensureHtml(text: String) {
        val hasAds = text.contains("data-adid=", ignoreCase = true)
        val challenged = text.contains("cf-challenge", ignoreCase = true) ||
                text.contains("Just a moment", ignoreCase = true) ||
                text.contains("Attention Required", ignoreCase = true)
        if (challenged && !hasAds) {
            throw KleinanzeigenBlockedException("Kleinanzeigen challenge/block page")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class KleinanzeigenBlockedException(message: String) : Exception(message)
