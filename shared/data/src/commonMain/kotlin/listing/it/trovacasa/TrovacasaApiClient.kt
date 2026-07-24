package listing.it.trovacasa

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * TrovaCasa.it — SSR HTML list + detail. See tmp/it/api/trovacasa/NOTES.md.
 */
class TrovacasaApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        citySlug: String,
        isSale: Boolean,
        page: Int,
    ): String {
        val operation = if (isSale) "case-in-vendita" else "case-in-affitto"
        val pageSuffix = if (page > 1) "?page=$page" else ""
        val url = "https://www.trovacasa.it/$operation/$citySlug$pageSuffix"
        return httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "it-IT,it;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.trovacasa.it/")
        }.bodyAsText()
    }

    suspend fun fetchDetailHtml(pathOrUrl: String): String {
        val path = when {
            pathOrUrl.startsWith("http") -> pathOrUrl
            pathOrUrl.startsWith("/") -> "https://www.trovacasa.it$pathOrUrl"
            else -> "https://www.trovacasa.it/$pathOrUrl"
        }
        return httpClient.get(path) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "it-IT,it;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.trovacasa.it/")
        }.bodyAsText()
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
