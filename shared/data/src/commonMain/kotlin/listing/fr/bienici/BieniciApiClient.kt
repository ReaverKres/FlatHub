package listing.fr.bienici

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Bien'ici public JSON — see tmp/fr/api/bienici/NOTES.md.
 */
class BieniciApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchSearch(
        isSale: Boolean,
        locationName: String,
        from: Int,
        size: Int = 24,
        roomsMin: Int? = null,
        roomsMax: Int? = null,
    ): JsonObject {
        val roomsPart = buildString {
            if (roomsMin != null) append(""","roomsMin":$roomsMin""")
            if (roomsMax != null) append(""","roomsMax":$roomsMax""")
        }
        val filters =
            """{"size":$size,"from":$from,"filterType":"${if (isSale) "buy" else "rent"}","propertyType":["flat"],"locationNames":["${
                locationName.replace(
                    "\"",
                    ""
                )
            }"]$roomsPart}"""
        val text = httpClient.get("https://www.bienici.com/realEstateAds.json") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "fr-FR,fr;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.bienici.com/")
            parameter("filters", filters)
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).jsonObject
    }

    suspend fun fetchDetail(externalId: String): JsonObject {
        val text = httpClient.get("https://www.bienici.com/realEstateAd.json") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "fr-FR,fr;q=0.9,en;q=0.8")
            header(HttpHeaders.Referrer, "https://www.bienici.com/")
            parameter("id", externalId)
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).jsonObject
    }

    private fun ensureJson(text: String) {
        val t = text.trimStart()
        if (!t.startsWith("{") && !t.startsWith("[")) {
            throw BieniciBlockedException("Bien'ici non-JSON response")
        }
        val lower = text.lowercase()
        if (lower.contains("datadome") || lower.contains("captcha")) {
            throw BieniciBlockedException("Bien'ici captcha/WAF")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class BieniciBlockedException(message: String) : Exception(message)
