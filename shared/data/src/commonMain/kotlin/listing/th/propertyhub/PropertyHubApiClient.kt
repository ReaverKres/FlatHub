package listing.th.propertyhub

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * PropertyHub Next.js `_next/data/{buildId}/…json`.
 * See tmp/th/api/propertyhub/NOTES.md.
 */
class PropertyHubApiClient(
    private val httpClient: HttpClient,
) {
    private val mutex = Mutex()
    @Volatile
    private var cachedBuildId: String? = null

    suspend fun fetchSearchJson(zoneSlug: String, isSale: Boolean, page: Int): String {
        val buildId = resolveBuildId()
        val kind = if (isSale) "condo-for-sale" else "condo-for-rent"
        val pageQs = if (page > 1) "?page=$page" else ""
        val url = "https://propertyhub.in.th/_next/data/$buildId/en/$kind/$zoneSlug.json$pageQs"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,th;q=0.8")
            header(HttpHeaders.Referrer, "https://propertyhub.in.th/en")
        }.bodyAsText()
        ensureJson(text)
        return text
    }

    private suspend fun resolveBuildId(): String = mutex.withLock {
        cachedBuildId?.let { return it }
        val html = httpClient.get("https://propertyhub.in.th/en") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9,th;q=0.8")
        }.bodyAsText()
        if (html.contains("403 Forbidden", ignoreCase = true) && !html.contains("buildId")) {
            throw PropertyHubBlockedException("PropertyHub homepage 403")
        }
        val id = BUILD_ID_RE.find(html)?.groupValues?.get(1)
            ?: FALLBACK_BUILD_ID
        cachedBuildId = id
        id
    }

    fun invalidateBuildId() {
        cachedBuildId = null
    }

    private fun ensureJson(text: String) {
        val lower = text.lowercase()
        if (lower.contains("just a moment") || lower.contains("cf-mitigated")) {
            throw PropertyHubBlockedException("PropertyHub captcha/WAF")
        }
        if (text.contains("403 Forbidden") && !text.trimStart().startsWith("{")) {
            throw PropertyHubBlockedException("PropertyHub 403")
        }
        if (!text.trimStart().startsWith("{") || !text.contains("pageProps")) {
            invalidateBuildId()
            throw PropertyHubBlockedException("PropertyHub: missing pageProps JSON")
        }
    }

    companion object {
        private val BUILD_ID_RE = Regex(""""buildId"\s*:\s*"([^"]+)"""")
        private const val FALLBACK_BUILD_ID =
            "propertyhub-web-dd4039b8d3fef924b88f911241be21c01202e9bc"
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1"
    }
}

class PropertyHubBlockedException(message: String) : Exception(message)
