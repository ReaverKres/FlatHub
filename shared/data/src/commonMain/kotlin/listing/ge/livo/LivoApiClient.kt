package listing.ge.livo

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Livo.ge public statements API. See tmp/ge/api/livo/NOTES.md.
 */
class LivoApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchStatements(
        page: Int,
        cityId: Int,
        dealType: Int,
        realEstateType: Int = REAL_ESTATE_APARTMENT,
        perPage: Int = 40,
        priceFrom: Int? = null,
        priceTo: Int? = null,
        /** 1 = GEL, 2 = USD (observed) */
        currencyId: Int = CURRENCY_USD,
    ): JsonObject {
        val text = httpClient.get("$BASE/v1/statements") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
            parameter("page", page)
            parameter("per_page", perPage)
            parameter("deal_types[]", dealType)
            parameter("real_estate_types[]", realEstateType)
            parameter("cities[]", cityId)
            if (priceFrom != null || priceTo != null) {
                parameter("currency_id", currencyId)
            }
            if (priceFrom != null) parameter("price_from", priceFrom)
            if (priceTo != null) parameter("price_to", priceTo)
        }.bodyAsText()
        return json.parseToJsonElement(text) as JsonObject
    }

    suspend fun fetchStatement(id: Long): JsonObject {
        val text = httpClient.get("$BASE/v1/statements/$id") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "ka,en;q=0.9")
        }.bodyAsText()
        return json.parseToJsonElement(text) as JsonObject
    }

    companion object {
        private const val BASE = "https://api.livo.ge"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        const val REAL_ESTATE_APARTMENT = 1
        const val CURRENCY_GEL = 1
        const val CURRENCY_USD = 2

        /** 1 sale, 2 rent, 7 daily (from statement-parameters) */
        const val DEAL_SALE = 1
        const val DEAL_RENT = 2
        const val DEAL_DAILY = 7
    }
}
