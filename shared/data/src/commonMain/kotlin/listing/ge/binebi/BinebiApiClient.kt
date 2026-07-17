package listing.ge.binebi

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Binebi.ge — listings via CSRF POST (GET HTML is filter shell only).
 * See tmp/ge/api/binebi/NOTES.md. Apartment `home_types` id is **985** (not 1).
 */
class BinebiApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchSearch(
        page: Int,
        cityId: Int,
        dealType: Int,
        homeType: Int = HOME_TYPE_APARTMENT,
    ): JsonObject {
        val session = openSession()
        val filterJson = buildJsonObject {
            put("deal_types", dealType)
            put("home_types", homeType)
            put("city", cityId)
        }.toString()
        val text = httpClient.submitForm(
            url = "$BASE/gancxadebebi",
            formParameters = Parameters.build {
                append("filter", filterJson)
                append("sort", "added")
                append("page", page.coerceAtLeast(1).toString())
                append("listing_total", "0")
                append("_token", session.csrf)
            },
        ) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/javascript, */*; q=0.01")
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
            header("X-Requested-With", "XMLHttpRequest")
            header("X-CSRF-TOKEN", session.csrf)
            header(HttpHeaders.Referrer, "$BASE/gancxadebebi")
            if (session.cookieHeader.isNotBlank()) {
                header(HttpHeaders.Cookie, session.cookieHeader)
            }
        }.bodyAsText()
        return json.parseToJsonElement(text) as JsonObject
    }

    suspend fun fetchDetailHtml(pathOrUrl: String): String {
        val url = when {
            pathOrUrl.startsWith("http") -> pathOrUrl
            pathOrUrl.startsWith("/") -> "$BASE$pathOrUrl"
            else -> "$BASE/$pathOrUrl"
        }
        return httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
        }.bodyAsText()
    }

    private suspend fun openSession(): Session {
        val response = httpClient.get("$BASE/gancxadebebi") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
            parameter("deal_types", DEAL_RENT)
            parameter("home_types", HOME_TYPE_APARTMENT)
            parameter("city", 1)
            parameter("sort", "added")
        }
        val html = response.bodyAsText()
        val csrf = CSRF_META.find(html)?.groupValues?.get(1)
            ?: CSRF_INPUT.find(html)?.groupValues?.get(1)
            ?: error("Binebi CSRF token not found")
        return Session(csrf = csrf, cookieHeader = cookieHeader(response))
    }

    private fun cookieHeader(response: HttpResponse): String =
        response.headers.getAll(HttpHeaders.SetCookie)
            ?.map { it.substringBefore(';').trim() }
            ?.filter { it.isNotEmpty() }
            ?.joinToString("; ")
            .orEmpty()

    private data class Session(val csrf: String, val cookieHeader: String)

    companion object {
        private const val BASE = "https://binebi.ge"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        private val CSRF_META =
            Regex("""name="csrf-token"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE)
        private val CSRF_INPUT =
            Regex("""name="_token"\s+value="([^"]+)"""", RegexOption.IGNORE_CASE)

        /** Flat / apartment (confirmed live; `1` returns empty). */
        const val HOME_TYPE_APARTMENT = 985
        const val DEAL_SALE = 1
        const val DEAL_RENT = 3
        const val DEAL_DAILY = 4
    }
}
