package listing.es.fotocasa

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull

/**
 * Fotocasa gateway REST (not HTML — Geetest blocks www pages).
 * See tmp/es/api/fotocasa/NOTES.md.
 */
class FotocasaApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun search(
        location: FotocasaCities.CityLocation,
        transactionTypeId: Int,
        pageNumber: Int,
        resultsPerPage: Int,
        priceFrom: Int? = null,
        priceTo: Int? = null,
    ): JsonObject {
        val text = httpClient.get("$BASE/v2/PropertySearch/Search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.AcceptLanguage, "es-ES,es;q=0.9")
            header(HttpHeaders.Origin, "https://www.fotocasa.es")
            header(HttpHeaders.Referrer, "https://www.fotocasa.es/")
            parameter("combinedLocationIds", location.combinedLocationIds)
            parameter("culture", "es-ES")
            parameter("homepage", "fotocasa")
            parameter("isMap", false)
            parameter("isNewConstruction", false)
            parameter("latitude", location.latitude)
            parameter("longitude", location.longitude)
            parameter("pageNumber", pageNumber)
            parameter("platformId", 1)
            parameter("propertyTypeId", PROPERTY_HOMES)
            parameter("resultsPerPage", resultsPerPage)
            parameter("sortOrderDesc", true)
            parameter("sortType", "scoring")
            parameter("transactionTypeId", transactionTypeId)
            if (priceFrom != null) parameter("minPrice", priceFrom)
            if (priceTo != null) parameter("maxPrice", priceTo)
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: error("Fotocasa search: not a JSON object")
    }

    suspend fun detail(transactionTypeId: Int, propertyId: Long): JsonObject {
        val text = httpClient.get("$BASE/v2/ad-details/${transactionTypeId}_$propertyId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.AcceptLanguage, "es-ES,es;q=0.9")
            header(HttpHeaders.Origin, "https://www.fotocasa.es")
            header(HttpHeaders.Referrer, "https://www.fotocasa.es/")
        }.bodyAsText()
        ensureJson(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: error("Fotocasa detail: not a JSON object")
    }

    private fun ensureJson(text: String) {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<") ||
            trimmed.contains("geetest", ignoreCase = true) ||
            trimmed.contains("SENTIMOS LA INTERRUPCIÓN", ignoreCase = true)
        ) {
            throw FotocasaBlockedException("Fotocasa gateway returned HTML/Geetest instead of JSON")
        }
    }

    companion object {
        const val TX_RENT = 3
        const val TX_SALE = 1
        const val PROPERTY_HOMES = 2
        private const val BASE = "https://web.gw.fotocasa.es"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

class FotocasaBlockedException(message: String) : Exception(message)
