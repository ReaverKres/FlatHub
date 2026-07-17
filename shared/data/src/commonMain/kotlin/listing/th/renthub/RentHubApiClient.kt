package listing.th.renthub

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * RentHub Next.js `_next/data/{buildId}/en/apartment/{zone}.json`.
 * See tmp/th/api/renthub/NOTES.md.
 */
class RentHubApiClient(
    private val httpClient: HttpClient,
) {
    private val mutex = Mutex()
    @Volatile
    private var cachedBuildId: String? = null

    suspend fun fetchSearchJson(zoneSlug: String, page: Int): String {
        val buildId = resolveBuildId()
        val pageQs = if (page > 1) "?page=$page" else ""
        val url =
            "https://www.renthub.in.th/_next/data/$buildId/en/apartment/$zoneSlug.json$pageQs"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,th;q=0.8")
            header(HttpHeaders.Referrer, "https://www.renthub.in.th/en")
        }.bodyAsText()
        ensureJson(text)
        return text
    }

    suspend fun fetchDetailJson(slug: String): String {
        val buildId = resolveBuildId()
        val url = "https://www.renthub.in.th/_next/data/$buildId/en/$slug.json"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,th;q=0.8")
            header(HttpHeaders.Referrer, "https://www.renthub.in.th/en")
        }.bodyAsText()
        ensureJson(text)
        return text
    }

    private suspend fun resolveBuildId(): String = mutex.withLock {
        cachedBuildId?.let { return it }
        val html = httpClient.get("https://www.renthub.in.th/en") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,th;q=0.8")
        }.bodyAsText()
        val id = BUILD_ID_RE.find(html)?.groupValues?.get(1)
            ?: FALLBACK_BUILD_ID
        cachedBuildId = id
        id
    }

    fun invalidateBuildId() {
        cachedBuildId = null
    }

    private fun ensureJson(text: String) {
        if (!text.trimStart().startsWith("{") || !text.contains("pageProps")) {
            invalidateBuildId()
            throw RentHubBlockedException("RentHub: missing pageProps JSON")
        }
    }

    companion object {
        private val BUILD_ID_RE = Regex(""""buildId"\s*:\s*"([^"]+)"""")
        private const val FALLBACK_BUILD_ID =
            "renthub-web-80765829c26f97dd2bba007f25520741b19f8b50"
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1"
    }
}

class RentHubBlockedException(message: String) : Exception(message)
