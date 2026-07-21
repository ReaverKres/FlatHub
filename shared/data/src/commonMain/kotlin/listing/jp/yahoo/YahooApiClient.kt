package listing.jp.yahoo

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.FeedDelayListBoost
import listing.core.asObjectOrNull
import listing.jp.JpCities

/**
 * Yahoo!不動産 — rent JSON API + used-mansion sale SSR HTML.
 * See tmp/jp/api/yahoo/NOTES.md
 */
class YahooApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchRentSearch(
        city: CityCode?,
        page: Int,
    ): JsonObject? {
        val ids = JpCities.ids(city)
        val pageSize = FeedDelayListBoost.apiPageSize(FlatPlatform.YAHOO_RE, DEFAULT_PAGE_SIZE)
        val response = httpClient.get("$BASE/rent/apj/api/search/") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/rent/search/")
            parameter("pref", ids.prefCode)
            parameter("results", pageSize.toString())
            parameter("page", page.coerceAtLeast(1).toString())
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            println("YahooApiClient rent HTTP ${response.status.value}: ${text.take(180)}")
            return null
        }
        if (text.trimStart().startsWith("<")) {
            throw YahooBlockedException("Yahoo rent returned HTML")
        }
        return json.parseToJsonElement(text).asObjectOrNull()
    }

    suspend fun fetchSaleSearchHtml(
        city: CityCode?,
        page: Int,
    ): String {
        val ids = JpCities.ids(city)
        val text = if (page <= 1) {
            val path = buildString {
                append("$BASE/used/mansion/search/${ids.yahooLc}/${ids.prefCode}")
                ids.yahooGeo?.let { append("/$it") }
                append("/")
            }
            httpClient.get(path) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "text/html")
                header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
                header(HttpHeaders.Referrer, "$BASE/used/mansion/")
            }.bodyAsText()
        } else {
            httpClient.get("$BASE/used/mansion/search/partials/") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "text/html")
                header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
                header(HttpHeaders.Referrer, "$BASE/used/mansion/")
                parameter("lc", ids.yahooLc)
                parameter("pf", ids.prefCode)
                ids.yahooGeo?.let { parameter("geo", it) }
                parameter("page", page.coerceAtLeast(1).toString())
            }.bodyAsText()
        }
        ensureHtml(text)
        return text
    }

    suspend fun fetchSaleDetailHtml(detailUrl: String): String {
        val url = if (detailUrl.startsWith("http")) detailUrl else "$BASE$detailUrl"
        val text = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/used/mansion/")
        }.bodyAsText()
        ensureHtml(text)
        return text
    }

    private fun ensureHtml(text: String) {
        if (text.trimStart().startsWith("{")) {
            throw YahooBlockedException("Yahoo sale returned JSON instead of HTML")
        }
        if (text.length < 5_000 && (
                    text.contains("403", ignoreCase = true) ||
                            text.contains("Forbidden", ignoreCase = true) ||
                            text.contains("アクセスが拒否", ignoreCase = true) ||
                            text.contains("common.bot", ignoreCase = true)
                    )
        ) {
            throw YahooBlockedException("Yahoo blocked or empty HTML response")
        }
    }

    companion object {
        const val BASE = "https://realestate.yahoo.co.jp"
        private const val DEFAULT_PAGE_SIZE = 30
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class YahooBlockedException(message: String) : Exception(message)
