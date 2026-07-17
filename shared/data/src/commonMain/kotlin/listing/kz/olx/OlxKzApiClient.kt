package listing.kz.olx

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OlxKzApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchOffers(
        categoryId: Int,
        regionId: Int,
        cityId: Int,
        offset: Int,
        limit: Int = 40,
        priceFrom: Int? = null,
        priceTo: Int? = null,
    ): JsonObject {
        val text = httpClient.get("https://www.olx.kz/api/v1/offers/") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("category_id", categoryId)
            parameter("region_id", regionId)
            parameter("city_id", cityId)
            if (priceFrom != null) parameter("filter_float_price:from", priceFrom)
            if (priceTo != null) parameter("filter_float_price:to", priceTo)
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    suspend fun fetchOffer(offerId: Long): JsonObject {
        val text = httpClient.get("https://www.olx.kz/api/v1/offers/$offerId/") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    /** Anonymous phones endpoint (works on KZ; see NOTES). Soft-fail if blocked. */
    suspend fun fetchPhones(offerId: Long): List<String> {
        val text = httpClient.get("https://www.olx.kz/api/v1/offers/$offerId/phones/") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
        }.bodyAsText()
        val root = json.parseToJsonElement(text).jsonObject
        val phones = root["data"]?.jsonObject?.get("phones")?.jsonArray ?: return emptyList()
        return phones.mapNotNull { it.jsonPrimitive.contentOrNull }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
