package listing.es.pisos

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * pisos.com — SSR HTML list + detail. See tmp/es/api/pisos/NOTES.md.
 */
class PisosApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        citySlug: String,
        isSale: Boolean,
        page: Int,
    ): String {
        val operation = if (isSale) "venta" else "alquiler"
        val pageSuffix = if (page > 1) "$page/" else ""
        val url = "https://www.pisos.com/$operation/pisos-$citySlug/$pageSuffix"
        return httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "es-ES,es;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.pisos.com/")
        }.bodyAsText()
    }

    suspend fun fetchDetailHtml(detailPath: String): String {
        val path = if (detailPath.startsWith("http")) {
            detailPath
        } else {
            "https://www.pisos.com${if (detailPath.startsWith("/")) detailPath else "/$detailPath"}"
        }
        return httpClient.get(path) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "es-ES,es;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.pisos.com/")
        }.bodyAsText()
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
