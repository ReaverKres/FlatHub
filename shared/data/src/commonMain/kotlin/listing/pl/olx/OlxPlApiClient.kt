package listing.pl.olx

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class OlxPlApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchOffers(
        categoryId: Int,
        regionId: Int,
        cityId: Int,
        offset: Int,
        limit: Int = 40,
        filterRefiners: String? = null,
    ): JsonObject {
        val text = httpClient.get("https://www.olx.pl/api/v1/offers/") {
            header(HttpHeaders.Accept, "application/json")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            header(HttpHeaders.AcceptLanguage, "pl-PL,pl;q=0.9")
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("category_id", categoryId)
            parameter("region_id", regionId)
            parameter("city_id", cityId)
            if (filterRefiners != null) {
                parameter("filter_refiners", filterRefiners)
            }
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    suspend fun fetchOffer(offerId: Long): JsonObject {
        val text = httpClient.get("https://www.olx.pl/api/v1/offers/$offerId/") {
            header(HttpHeaders.Accept, "application/json")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            header(HttpHeaders.AcceptLanguage, "pl-PL,pl;q=0.9")
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }
}
