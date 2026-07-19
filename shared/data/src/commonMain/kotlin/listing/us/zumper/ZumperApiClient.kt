package listing.us.zumper

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * Zumper US SSR HTML → `window.__PRELOADED_STATE__`.
 * See tmp/us/api/zumper/NOTES.md.
 */
class ZumperApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchSearchHtml(
        slug: String,
        page: Int,
    ): String {
        val base = "https://www.zumper.com/apartments-for-rent/$slug"
        val url = if (page > 1) "$base?page=$page" else base
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.zumper.com/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    suspend fun fetchDetailHtml(pathOrUrl: String): String {
        val url = when {
            pathOrUrl.startsWith("http") -> pathOrUrl
            pathOrUrl.startsWith("/") -> "https://www.zumper.com$pathOrUrl"
            else -> "https://www.zumper.com/$pathOrUrl"
        }
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            header(HttpHeaders.Referrer, "https://www.zumper.com/")
        }.bodyAsText()
        ensureHtml(text, requirePreloadedState = false)
        return text
    }

    private fun ensureHtml(text: String, requirePreloadedState: Boolean = true) {
        val lower = text.lowercase()
        val looksBlocked =
            lower.contains("cf-mitigated") ||
                    lower.contains("just a moment") ||
                    (lower.contains("attention required") && lower.contains("cloudflare")) ||
                    lower.contains("pardon our interruption") ||
                    (lower.contains("access denied") && !text.contains("__PRELOADED_STATE__"))
        if (looksBlocked) {
            throw ZumperBlockedException("Zumper captcha/WAF")
        }
        if (requirePreloadedState && !text.contains("__PRELOADED_STATE__")) {
            throw ZumperBlockedException("Zumper: missing __PRELOADED_STATE__")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class ZumperBlockedException(message: String) : Exception(message)
