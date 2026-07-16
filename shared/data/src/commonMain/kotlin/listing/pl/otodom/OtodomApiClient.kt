package listing.pl.otodom

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Otodom Next.js data client.
 * buildId is discovered from search HTML `__NEXT_DATA__` and cached.
 */
class OtodomApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    @Volatile
    private var cachedBuildId: String? = null

    suspend fun fetchSearchJson(
        transactionPath: String, // wynajem | sprzedaz
        estatePath: String, // mieszkanie
        cityPath: String,
        page: Int,
        priceMin: Int?,
        priceMax: Int?,
    ): JsonObject {
        val buildId = ensureBuildId()
        val query = buildString {
            append("page=$page&limit=36")
            if (priceMin != null) append("&priceMin=$priceMin")
            if (priceMax != null) append("&priceMax=$priceMax")
        }
        val path =
            "/_next/data/$buildId/pl/wyniki/$transactionPath/$estatePath/$cityPath.json?$query"
        val url = "https://www.otodom.pl$path"
        val text = httpClient.get(url) {
            header("x-nextjs-data", "1")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "pl-PL,pl;q=0.9")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    private suspend fun ensureBuildId(): String {
        cachedBuildId?.let { return it }
        val html = httpClient.get(
            "https://www.otodom.pl/pl/wyniki/wynajem/mieszkanie/mazowieckie/warszawa/warszawa/warszawa",
        ) {
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
        }.bodyAsText()
        val marker = "\"buildId\":\""
        val start = html.indexOf(marker)
        require(start >= 0) { "Otodom buildId not found in HTML" }
        val from = start + marker.length
        val end = html.indexOf('"', from)
        val id = html.substring(from, end)
        cachedBuildId = id
        return id
    }

    fun invalidateBuildId() {
        cachedBuildId = null
    }

    companion object {
        fun readBuildIdFromNextData(json: JsonObject): String? =
            json["buildId"]?.jsonPrimitive?.contentOrNull
    }
}
