package listing.pl.morizon

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class MorizonApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    suspend fun searchProperties(listingUrl: String): JsonObject {
        val body = buildJsonObject {
            put("query", SEARCH_QUERY)
            put(
                "variables",
                buildJsonObject {
                    put("url", listingUrl)
                },
            )
        }
        val text = httpClient.post("https://www.morizon.pl/api-morizon") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Origin, "https://www.morizon.pl")
            header("Referer", "https://www.morizon.pl/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            setBody(json.encodeToString(JsonObject.serializer(), body))
        }.bodyAsText()
        return json.parseToJsonElement(text).jsonObject
    }

    companion object {
        private val SEARCH_QUERY =
            """
            query getPropertyListingData(${'$'}url: String!) {
              searchResult: searchProperties(url: ${'$'}url) {
                properties {
                  totalCount
                  nodes {
                    id
                    idOnFrontend
                    title
                    url
                    addedAt
                    refreshedAt
                    area
                    numberOfRooms
                    floorFormatted
                    description(maxLength: 200)
                    price { amount currency }
                    location { location street }
                    photos { id name alt }
                    contact {
                      person { name phones type }
                      company { name phones type }
                    }
                  }
                }
              }
            }
            """.trimIndent()
    }
}
