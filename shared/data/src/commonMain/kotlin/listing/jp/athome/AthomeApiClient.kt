package listing.jp.athome

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import listing.jp.JpCities

/**
 * at home — rent HTML + sale BFF JSON. See tmp/jp/api/athome/NOTES.md
 */
class AthomeApiClient(
    private val httpClient: HttpClient,
) {
    suspend fun fetchRentListHtml(city: CityCode?, page: Int): String {
        val slug = JpCities.ids(city).athomeSlug
        val path = if (page <= 1) {
            "$BASE/chintai/$slug/list/"
        } else {
            "$BASE/chintai/$slug/list/page$page/"
        }
        val response = httpClient.get(path) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/chintai/")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess() || response.status.value == 405) {
            throw AthomeBlockedException("Athome rent list HTTP ${response.status.value}")
        }
        ensureNotBlocked(text, "Athome rent list")
        return text
    }

    suspend fun fetchSaleListJson(city: CityCode?, page: Int, seoNm: String): String {
        val slug = JpCities.ids(city).athomeSlug
        val response = httpClient.get("$BASE/csite-bff/sell-living/bukken/list/first-view") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/kodate/$slug/list/")
            parameter("basicConditions", "kp299,kb206,kb208,kt424")
            parameter("bklistId", "001LPC")
            parameter("breadcrumbCd", "20")
            parameter("num", "30")
            parameter("page", page.coerceAtLeast(1).toString())
            parameter("pageSetting", "012400")
            parameter("prefectureRoman", slug)
            parameter("seoNm", seoNm)
            parameter("siteCd", "00000")
            parameter("sort_id", "95")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess() || response.status.value == 405) {
            throw AthomeBlockedException("Athome sale BFF HTTP ${response.status.value}")
        }
        if (text.trimStart().startsWith("<")) {
            ensureNotBlocked(text, "Athome sale BFF")
        }
        return text
    }

    suspend fun fetchRentDetailHtml(bukkenNo: String): String {
        val response = httpClient.get("$BASE/chintai/$bukkenNo/") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/chintai/")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess() || response.status.value == 405) {
            throw AthomeBlockedException("Athome rent detail HTTP ${response.status.value}")
        }
        ensureNotBlocked(text, "Athome rent detail")
        return text
    }

    suspend fun fetchSaleDetailHtml(bukkenNo: String, seoRoma: String): String {
        val segment = seoRoma.substringBefore('/').ifBlank { "kodate" }
        val response = httpClient.get("$BASE/$segment/$bukkenNo/") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9")
            header(HttpHeaders.Referrer, "$BASE/$segment/")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess() || response.status.value == 405) {
            throw AthomeBlockedException("Athome sale detail HTTP ${response.status.value}")
        }
        ensureNotBlocked(text, "Athome sale detail")
        return text
    }

    suspend fun fetchSearchPayload(city: CityCode?, adType: AdType, page: Int): String =
        when (adType) {
            is AdType.SALE -> fetchSaleListJson(city, page, seoNm = "kodate")
            else -> fetchRentListHtml(city, page)
        }

    companion object {
        const val BASE = "https://www.athome.co.jp"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        fun ensureNotBlocked(text: String, context: String) {
            if (isBotWall(text)) {
                throw AthomeBlockedException("$context: Reese/Geetest challenge")
            }
        }

        fun isBotWall(text: String): Boolean =
            text.contains("認証にご協力") ||
                    text.contains("eadjaxlayqcmrfpo") ||
                    text.contains("Geetest", ignoreCase = true) ||
                    text.contains("Click to verify", ignoreCase = true)
    }
}

class AthomeBlockedException(message: String) : Exception(message)
