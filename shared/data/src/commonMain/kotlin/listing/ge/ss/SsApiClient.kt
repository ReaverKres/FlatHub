package listing.ge.ss

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.concurrent.Volatile

/**
 * SS.ge / home.ss.ge RealEstate LegendSearch client.
 * Auth: Bearer [credentialsToken] from `__NEXT_DATA__` + session cookies.
 * See tmp/ge/api/ss/NOTES.md.
 */
class SsApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedCookie: String? = null

    suspend fun legendSearch(
        page: Int,
        cityId: Int,
        dealType: Int,
        realEstateType: Int = REAL_ESTATE_FLAT,
        pageSize: Int = 40,
    ): JsonObject {
        ensureAuth()
        val body = buildJsonObject {
            put("page", page)
            put("pageSize", pageSize)
            putJsonArray("cityIdList") { add(JsonPrimitive(cityId)) }
            put("realEstateDealType", dealType)
            put("realEstateType", realEstateType)
        }
        return try {
            postLegend(body)
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e is SsAuthException) {
                invalidateAuth()
                ensureAuth()
                postLegend(body)
            } else {
                throw e
            }
        }
    }

    fun invalidateAuth() {
        cachedToken = null
        cachedCookie = null
    }

    private suspend fun postLegend(body: JsonObject): JsonObject {
        val token = cachedToken ?: error("SS.ge token missing")
        val response = httpClient.post(LEGEND_SEARCH_URL) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.AcceptLanguage, "ka")
            header(HttpHeaders.Origin, ORIGIN)
            header(HttpHeaders.Referrer, HOME_URL)
            header(HttpHeaders.Authorization, "Bearer $token")
            cachedCookie?.let { header(HttpHeaders.Cookie, it) }
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        if (response.status.value == 401 || text.isBlank()) {
            throw SsAuthException("SS.ge LegendSearch unauthorized")
        }
        if (!text.trimStart().startsWith("{")) {
            error("SS.ge LegendSearch non-JSON: ${text.take(120)}")
        }
        return json.parseToJsonElement(text) as JsonObject
    }

    private suspend fun ensureAuth() {
        if (cachedToken != null) return
        val htmlResponse = httpClient.get(HOME_URL) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
        }
        val setCookies = htmlResponse.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        cachedCookie = setCookies.joinToString("; ") { cookie ->
            cookie.substringBefore(';').trim()
        }.ifBlank { null }
        val html = htmlResponse.bodyAsText()
        val token = extractCredentialsToken(html)
            ?: error("SS.ge credentialsToken not found in __NEXT_DATA__")
        cachedToken = token
    }

    companion object {
        private const val ORIGIN = "https://home.ss.ge"
        private const val HOME_URL = "https://home.ss.ge/ka/udzravi-qoneba"
        private const val LEGEND_SEARCH_URL =
            "https://api-gateway.ss.ge/v1/RealEstate/LegendSearch"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        const val REAL_ESTATE_FLAT = 5
        const val DEAL_RENT = 1
        const val DEAL_LEASE = 2
        const val DEAL_DAILY = 3
        const val DEAL_SALE = 4

        fun extractCredentialsToken(html: String): String? {
            val marker = "\"credentialsToken\":\""
            val start = html.indexOf(marker)
            if (start < 0) return null
            val from = start + marker.length
            val end = html.indexOf('"', from)
            if (end <= from) return null
            return html.substring(from, end)
        }
    }
}

private class SsAuthException(message: String) : Exception(message)
