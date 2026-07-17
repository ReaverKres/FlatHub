package listing.kz.krisha

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Krisha.kz — SSR HTML list + detail. See tmp/kz/api/krisha/NOTES.md.
 * Do not call phone AJAX anonymously (login wall + aggressive rate limits).
 */
class KrishaApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        cityAlias: String,
        isSale: Boolean,
        page: Int,
        rooms: Int? = null,
        priceFrom: Int? = null,
        priceTo: Int? = null,
    ): String {
        val section = if (isSale) "prodazha" else "arenda"
        return httpClient.get("https://krisha.kz/$section/kvartiry/$cityAlias/") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
            if (!isSale) parameter("das[rent.period]", "2")
            if (rooms != null && rooms > 0) parameter("das[live.rooms]", rooms)
            if (priceFrom != null) parameter("das[price][from]", priceFrom)
            if (priceTo != null) parameter("das[price][to]", priceTo)
            if (page > 1) parameter("page", page)
        }.bodyAsText()
    }

    suspend fun fetchDetailHtml(adId: Long): String {
        return httpClient.get("https://krisha.kz/a/show/$adId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
            header(HttpHeaders.Referrer, "https://krisha.kz/")
        }.bodyAsText()
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
