package listing.th.livinginsider

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.setCookie
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Livinginsider SSR HTML searchword. Needs session cookie from homepage.
 * See tmp/th/api/livinginsider/NOTES.md.
 */
class LivinginsiderApiClient(
    private val httpClient: HttpClient,
) {
    private val mutex = Mutex()
    @Volatile
    private var cookieHeader: String? = null

    suspend fun fetchSearchHtml(isSale: Boolean, page: Int): String {
        ensureSession()
        val deal = if (isSale) "Buysell" else "Rent"
        val slug = if (isSale) {
            "%E0%B8%A3%E0%B8%A7%E0%B8%A1%E0%B8%9B%E0%B8%A3%E0%B8%B0%E0%B8%81%E0%B8%B2%E0%B8%A8-%E0%B8%82%E0%B8%B2%E0%B8%A2-%E0%B8%84%E0%B8%AD%E0%B8%99%E0%B9%82%E0%B8%94.html"
        } else {
            "%E0%B8%A3%E0%B8%A7%E0%B8%A1%E0%B8%9B%E0%B8%A3%E0%B8%B0%E0%B8%81%E0%B8%B2%E0%B8%A8-%E0%B9%80%E0%B8%8A%E0%B9%88%E0%B8%B2-%E0%B8%84%E0%B8%AD%E0%B8%99%E0%B9%82%E0%B8%94.html"
        }
        val url = "https://www.livinginsider.com/searchword/Condo/$deal/$page/$slug"
        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "th-TH,th;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.livinginsider.com/")
            cookieHeader?.let { header(HttpHeaders.Cookie, it) }
        }
        mergeCookies(response)
        val text = response.bodyAsText()
        ensureSearchHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(detailUrl: String): String {
        ensureSession()
        val url = if (detailUrl.startsWith("http")) {
            detailUrl
        } else {
            "https://www.livinginsider.com${if (detailUrl.startsWith("/")) detailUrl else "/$detailUrl"}"
        }
        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "th-TH,th;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.livinginsider.com/")
            cookieHeader?.let { header(HttpHeaders.Cookie, it) }
        }
        mergeCookies(response)
        return response.bodyAsText()
    }

    private suspend fun ensureSession() {
        mutex.withLock {
            if (cookieHeader != null) return
            val response = httpClient.get("https://www.livinginsider.com/") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "text/html")
                header(HttpHeaders.AcceptLanguage, "th-TH,th;q=0.9,en;q=0.8")
            }
            mergeCookies(response)
            if (cookieHeader.isNullOrBlank()) {
                throw LivinginsiderBlockedException("Livinginsider: no session cookie")
            }
        }
    }

    private fun mergeCookies(response: HttpResponse) {
        val parts = response.setCookie().map { "${it.name}=${it.value}" }
        if (parts.isEmpty()) return
        val existing = cookieHeader
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
            .associateBy { it.substringBefore('=') }
            .toMutableMap()
        for (p in parts) {
            existing[p.substringBefore('=')] = p
        }
        cookieHeader = existing.values.joinToString("; ")
    }

    private fun ensureSearchHtml(text: String) {
        if (!text.contains("text_price") && !text.contains("/detail/")) {
            cookieHeader = null
            throw LivinginsiderBlockedException("Livinginsider: empty search HTML")
        }
        // Soft-redirect to homepage loses listings.
        if (text.contains("ลงประกาศฟรี") && !text.contains("class=\"text_price\"")) {
            cookieHeader = null
            throw LivinginsiderBlockedException("Livinginsider redirected to home")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}

class LivinginsiderBlockedException(message: String) : Exception(message)
