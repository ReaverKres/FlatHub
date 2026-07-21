package listing.kr.zigbang

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import listing.kr.zigbang.ZigbangApiClient.Companion.MAX_ITEM_IDS

/**
 * Zigbang (직방) anonymous REST — `apis.zigbang.com`.
 * See tmp/kr/api/zigbang/NOTES.md.
 */
class ZigbangApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchMapSearchJson(
        segment: String,
        geohash: String,
        salesType: String,
    ): String {
        val text = httpClient.get("$BASE/v2/items/$segment") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, REFERER)
            parameter("geohash", geohash)
            parameter("domain", "zigbang")
            parameter("salesTypes[0]", salesType)
            if (segment == SEGMENT_ONEROOM && salesType != SALES_SALE) {
                parameter("depositMin", 0)
                parameter("rentMin", 0)
                parameter("checkAnyItemWithoutFilter", true)
            }
        }.bodyAsText()
        ensureJson(text)
        return text
    }

    /**
     * Batch list card payload. API hard-caps [itemIds] at [MAX_ITEM_IDS] (HTTP 400 otherwise).
     * Prefer this over N× [fetchDetailJson] for search; keep v3 detail for phones/gallery.
     */
    suspend fun fetchItemsListJson(itemIds: List<Long>): String {
        require(itemIds.isNotEmpty()) { "Zigbang items/list: empty itemIds" }
        require(itemIds.size <= MAX_ITEM_IDS) {
            "Zigbang items/list: at most $MAX_ITEM_IDS ids (got ${itemIds.size})"
        }
        val body = """{"itemIds":[${itemIds.joinToString(",")}]}"""
        val response = httpClient.post("$BASE/house/property/v1/items/list") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, REFERER)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ZigbangApiException(
                "Zigbang items/list HTTP ${response.status.value}: ${text.take(200)}",
            )
        }
        ensureJson(text)
        return text
    }

    suspend fun fetchDetailJson(itemId: Long): String {
        val response = httpClient.get("$BASE/v3/items/$itemId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Referrer, REFERER)
            parameter("domain", "zigbang")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ZigbangApiException("Zigbang detail HTTP ${response.status.value} for $itemId")
        }
        ensureJson(text)
        return text
    }

    private fun ensureJson(text: String) {
        if (!text.trimStart().startsWith("{")) {
            throw ZigbangApiException("Zigbang: expected JSON object")
        }
    }

    companion object {
        const val BASE = "https://apis.zigbang.com"
        const val REFERER = "https://www.zigbang.com/"
        const val SEGMENT_ONEROOM = "oneroom"
        const val SEGMENT_OFFICETEL = "officetel"
        const val SEGMENT_VILLA = "villa"
        const val SALES_RENT = "월세"
        const val SALES_JEONSE = "전세"
        const val SALES_SALE = "매매"

        /** Hard limit from API: `itemIds must contain no more than 15 elements`. */
        const val MAX_ITEM_IDS = 15
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}

class ZigbangApiException(message: String) : Exception(message)
