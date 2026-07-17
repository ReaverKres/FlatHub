package listing.de.immowelt

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Immowelt.de SSR HTML list (detail often DataDome-blocked). Soft-fail on captcha.
 */
class ImmoweltApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        city: ImmoweltCities.CityPath,
        isSale: Boolean,
        page: Int,
    ): String {
        val op = if (isSale) "kaufen" else "mieten"
        val pageQs = if (page > 1) "?page=$page" else ""
        val url =
            "https://www.immowelt.de/suche/$op/wohnung/${city.region}/${city.cityPlz}/${city.geoId}$pageQs"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-DE,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.immowelt.de/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        val text = httpClient.get(detailUrl) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "de-DE,de;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.immowelt.de/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    private fun ensureHtml(text: String) {
        if (text.contains("geo.captcha-delivery.com", ignoreCase = true) ||
            text.contains("datadome", ignoreCase = true) ||
            text.contains("captcha-delivery", ignoreCase = true)
        ) {
            throw ImmoweltBlockedException("Immowelt DataDome/captcha")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class ImmoweltBlockedException(message: String) : Exception(message)
