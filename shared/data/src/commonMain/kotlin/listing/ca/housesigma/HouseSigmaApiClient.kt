package listing.ca.housesigma

import io.ktor.client.HttpClient
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import listing.core.contentOrNull
import kotlin.time.Clock

/**
 * HouseSigma `bkv2/api` — token + encrypted list cards + signed map fallback.
 * See tmp/ca/api/housesigma/NOTES.md.
 */
class HouseSigmaApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private var accessToken: String? = null
    private var secretKey: String? = null
    private var tokenExpiresAtSec: Long = 0

    suspend fun fetchList(
        market: HouseSigmaCities.Market,
        listType: Int,
        page: Int = 1,
    ): JsonObject {
        val body = buildSearchBody(market, listType, page)
        return postEncrypted("/search/mapsearchv3/list", body, market.province)
    }

    suspend fun fetchDetail(idListing: String, province: String): JsonObject {
        val body = buildJsonObject {
            put("lang", "en_US")
            put("province", province)
            put("id_listing", idListing)
            put("id_address", "")
            put("event_source", "")
        }
        return postEncrypted("/listing/info/detail_v2", body, province)
    }

    suspend fun fetchMapListing(
        market: HouseSigmaCities.Market,
        listType: Int,
        page: Int = 1,
    ): JsonObject {
        ensureToken()
        val province = market.province
        val body = buildSearchBody(market, listType, page)
        val signed = signBody(body)
        val text = httpClient.post("$API_BASE/search/mapsearchv3/listing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("HS-Client-Type", CLIENT_TYPE)
            header("HS-Client-Version", CLIENT_VERSION)
            header("Hs-Current-URL", "https://housesigma.com/${province.lowercase()}/map/")
            header(HttpHeaders.Referrer, "https://housesigma.com/${province.lowercase()}/map/")
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(json.encodeToString(JsonObject.serializer(), signed))
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    private suspend fun ensureToken() {
        val now = Clock.System.now().epochSeconds
        if (accessToken != null && secretKey != null && now < tokenExpiresAtSec - 300) return
        val text = httpClient.post("$API_BASE/init/accesstoken/new") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("HS-Client-Type", CLIENT_TYPE)
            header("HS-Client-Version", CLIENT_VERSION)
            setBody("{}")
        }.bodyAsText()
        val root = json.parseToJsonElement(text).jsonObject
        val data = root["data"]?.jsonObject ?: throw HouseSigmaApiException("token: missing data")
        accessToken = data["access_token"]?.contentOrNull()
            ?: throw HouseSigmaApiException("token: missing access_token")
        secretKey = data["secret"]?.jsonObject
            ?.get("secret_key")
            ?.contentOrNull()
            ?: throw HouseSigmaApiException("token: missing secret_key")
        tokenExpiresAtSec = data["secret"]?.jsonObject
            ?.get("expired_at")
            ?.contentOrNull()
            ?.toLongOrNull()
            ?: (now + 86_400)
    }

    private fun buildSearchBody(
        market: HouseSigmaCities.Market,
        listType: Int,
        page: Int,
    ): JsonObject = buildJsonObject {
        put("lang", "en_US")
        put("province", market.province)
        put("page", page)
        put("lat1", market.bbox.lat1)
        put("lat2", market.bbox.lat2)
        put("lon1", market.bbox.lon1)
        put("lon2", market.bbox.lon2)
        put("zoom", 14)
        putJsonArray("list_type") { add(JsonPrimitive(listType)) }
        putJsonArray("house_type") { add(JsonPrimitive("all")) }
        put("listing_days", 0)
        put("sold_days", 90)
        put("de_list_days", 90)
        putJsonArray("basement") { }
        put("open_house_date", 0)
        put("description", "")
        putJsonArray("listing_type") { add(JsonPrimitive("all")) }
        put("max_maintenance_fee", 0)
        putJsonArray("bedroom") { add(JsonPrimitive(0)) }
        put("bathroom", 0)
        put("garage", 0)
        put("price_sale_min", 0)
        put("price_sale_max", 6_000_000)
        put("price_rent_min", 0)
        put("price_rent_max", 10_000)
        put("square_footage_min", 0)
        put("square_footage_max", 4000)
        put("lot_front_feet_min", 0)
        put("lot_front_feet_max", 100)
        put("lot_size_min", 0)
        put("lot_size_max", 10_000_000)
        put("building_age_min", 999)
        put("building_age_max", 0)
        put("rental_yield_min", 0)
        put("rental_yield_max", 0.1)
        put("school_score_min", 0)
        put("school_score_max", 10)
    }

    private fun signBody(body: JsonObject): JsonObject {
        val ts = Clock.System.now().epochSeconds.toString()
        val flat = body.entries.associate { it.key to it.value }.toMutableMap()
        flat["ts"] = JsonPrimitive(ts)
        val signature = HouseSigmaSigner.sign(ts, flat)
        return buildJsonObject {
            flat.forEach { (k, v) -> put(k, v) }
            put("signature", signature)
        }
    }

    private suspend fun postEncrypted(
        endpoint: String,
        body: JsonObject,
        province: String,
    ): JsonObject {
        ensureToken()
        val bodyJson = json.encodeToString(JsonObject.serializer(), body)
        val key = secretKey ?: throw HouseSigmaApiException("token: missing secret_key")
        val encrypted = HouseSigmaCrypto.encryptPayload(bodyJson, key)
        val text = httpClient.post("$API_BASE$endpoint") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("HS-Client-Type", CLIENT_TYPE)
            header("HS-Client-Version", CLIENT_VERSION)
            header("Hs-Current-URL", "https://housesigma.com/${province.lowercase()}/map/")
            header(HttpHeaders.Referrer, "https://housesigma.com/${province.lowercase()}/map/")
            header("Hs-Request-Timestamp", encrypted.requestTimestamp)
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(json.encodeToString(JsonObject.serializer(), encrypted.body))
        }.bodyAsText()
        val root = json.parseToJsonElement(text).jsonObject
        val dataEl = root["data"]
        return when (dataEl) {
            is JsonPrimitive -> {
                val plain = HouseSigmaCrypto.decryptResponse(
                    dataEl.content,
                    encrypted.aesKey,
                    encrypted.counter,
                )
                json.parseToJsonElement(plain).jsonObject
            }

            else -> root
        }
    }

    companion object {
        private const val API_BASE = "https://housesigma.com/bkv2/api"
        private const val CLIENT_TYPE = "desktop_v7"
        private const val CLIENT_VERSION = "7.22.28"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class HouseSigmaApiException(message: String) : Exception(message)
