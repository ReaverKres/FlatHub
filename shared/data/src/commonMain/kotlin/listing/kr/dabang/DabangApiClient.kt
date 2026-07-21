package listing.kr.dabang

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import listing.core.asObjectOrNull
import listing.core.intOrNull

/**
 * Dabang (다방) v5 JSON API — browser headers required.
 * See tmp/kr/api/dabang/NOTES.md.
 */
class DabangApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchRoomList(
        category: String,
        areaType: String,
        page: Int,
        filtersJson: JsonObject,
        bboxJson: String? = null,
        regionCode: String? = null,
    ): JsonObject? {
        val filtersParam = json.encodeToString(JsonObject.serializer(), filtersJson)
        val response = httpClient.get("$BASE/api/v5/room-list/category/$category/$areaType") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.AcceptLanguage, "ko-KR,ko;q=0.9,en;q=0.8")
            header("D-Api-Version", API_VERSION)
            header("D-Call-Type", "web")
            header(HttpHeaders.Referrer, "$BASE/")
            header(HttpHeaders.Origin, BASE)
            parameter("useMap", "naver")
            parameter("zoom", "15")
            parameter("page", page.coerceAtLeast(1).toString())
            parameter("filters", filtersParam)
            if (bboxJson != null) parameter("bbox", bboxJson)
            if (regionCode != null) parameter("code", regionCode)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            println("DabangApiClient list HTTP ${response.status.value}: ${text.take(180)}")
            if (response.status == HttpStatusCode.Forbidden) return null
            if (response.status == HttpStatusCode.BadRequest || text.contains("서비스가 지연")) {
                throw DabangBlockedException("Dabang list HTTP ${response.status.value}")
            }
            return null
        }
        return parseListResponse(text)
    }

    suspend fun fetchRoomDetail(roomId: String): JsonObject {
        val response = httpClient.get("$BASE/api/v5/room/$roomId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header("D-Api-Version", API_VERSION)
            header("D-Call-Type", "web")
            header(HttpHeaders.Referrer, "$BASE/")
            header(HttpHeaders.Origin, BASE)
        }
        if (response.status == HttpStatusCode.Forbidden) {
            throw DabangBlockedException("Dabang detail HTTP 403 for $roomId")
        }
        val text = response.bodyAsText()
        if (text.isBlank()) {
            throw DabangBlockedException("Dabang detail empty for $roomId")
        }
        ensureNotHtmlBlock(text)
        return json.parseToJsonElement(text).asObjectOrNull()
            ?: throw DabangBlockedException("Dabang detail: not a JSON object")
    }

    fun oneTwoRentFilters(rentType: String): JsonObject = buildJsonObject {
        putJsonArray("sellingTypeList") { add(JsonPrimitive(rentType)) }
        putJsonObject("depositRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonObject("priceRange") {
            put("min", 0)
            put("max", 999999)
        }
        put("isIncludeMaintenance", false)
        putJsonObject("pyeongRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonObject("useApprovalDateRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonArray("roomFloorList") {
            add(JsonPrimitive("GROUND_FIRST"))
            add(JsonPrimitive("GROUND_SECOND_OVER"))
            add(JsonPrimitive("SEMI_BASEMENT"))
            add(JsonPrimitive("ROOFTOP"))
        }
        putJsonArray("roomTypeList") {
            add(JsonPrimitive("ONE_ROOM"))
            add(JsonPrimitive("TWO_ROOM"))
        }
        // Include direct listings — AGENT-only often under-fills pages.
        putJsonArray("dealTypeList") {
            add(JsonPrimitive("AGENT"))
            add(JsonPrimitive("DIRECT"))
        }
        put("canParking", false)
        put("isShortLease", false)
        put("hasElevator", false)
        put("hasPano", false)
        put("isDivision", false)
        put("isDuplex", false)
    }

    fun officetelSaleFilters(): JsonObject = buildJsonObject {
        putJsonArray("sellingTypeList") { add(JsonPrimitive("SELL")) }
        putJsonObject("tradeRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonObject("depositRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonObject("priceRange") {
            put("min", 0)
            put("max", 999999)
        }
        put("isIncludeMaintenance", false)
        putJsonObject("pyeongRange") {
            put("min", 0)
            put("max", 50)
        }
        putJsonObject("useApprovalDateRange") {
            put("min", 0)
            put("max", 999999)
        }
        putJsonObject("parkingNumRange") {
            put("min", 0)
            put("max", 999999)
        }
        put("canParking", false)
        put("isShortLease", false)
        put("hasElevator", false)
        put("hasPano", false)
        putJsonArray("roomCountList") {
            add(JsonPrimitive("ONE_ROOM"))
            add(JsonPrimitive("TWO_ROOM"))
            add(JsonPrimitive("THREE_ROOM"))
            add(JsonPrimitive("FOUR_ROOM"))
        }
    }

    private fun parseListResponse(text: String): JsonObject? {
        ensureNotHtmlBlock(text)
        val root = runCatching { json.parseToJsonElement(text).asObjectOrNull() }.getOrNull()
            ?: return null
        val httpCode = root["code"].intOrNull()
        if (httpCode != null && httpCode != 200) {
            println("DabangApiClient list body code=$httpCode: ${text.take(180)}")
            if (httpCode == 403) return null
            if (httpCode == 400 || text.contains("서비스가 지연")) {
                throw DabangBlockedException("Dabang list HTTP $httpCode: ${text.take(200)}")
            }
        }
        return root
    }

    private fun ensureNotHtmlBlock(text: String) {
        if (text.trimStart().startsWith("<")) {
            throw DabangBlockedException("Dabang returned HTML instead of JSON")
        }
    }

    companion object {
        const val BASE = "https://www.dabangapp.com"
        private const val API_VERSION = "5.0.0"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class DabangBlockedException(message: String) : Exception(message)
