package listing.kz.kn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * kn.kz — SSR HTML + small XHR helpers. See tmp/kz/api/kn/NOTES.md.
 */
class KnApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun fetchSearchHtml(cityAlias: String, isSale: Boolean, page: Int): String {
        val section = if (isSale) "prodazha-kvartir" else "arenda-kvartir"
        return httpClient.get("https://www.kn.kz/$cityAlias/$section") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
            if (page > 1) parameter("page", page)
        }.bodyAsText()
    }

    suspend fun fetchDetailHtml(adId: Long): String {
        return httpClient.get("https://www.kn.kz/card/$adId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            header(HttpHeaders.AcceptLanguage, "ru-KZ,ru;q=0.9")
        }.bodyAsText()
    }

    suspend fun fetchMapHtml(adId: Long): String {
        return httpClient.get("https://www.kn.kz/card/map/$adId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "*/*")
            header("X-Requested-With", "XMLHttpRequest")
        }.bodyAsText()
    }

    suspend fun fetchPhones(adId: Long): List<String> {
        val text = httpClient.get("https://www.kn.kz/card/phone/$adId/0") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            header("X-Requested-With", "XMLHttpRequest")
        }.bodyAsText()
        val root = json.parseToJsonElement(text).jsonObject
        val phones = root["phones"]?.jsonArray ?: return emptyList()
        return phones.mapNotNull { it.jsonPrimitive.contentOrNull }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
