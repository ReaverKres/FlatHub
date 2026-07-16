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
    suspend fun searchProperties(listingUrl: String): JsonObject =
        postGraphql(SEARCH_QUERY, listingUrl)

    suspend fun fetchProperty(detailUrl: String): JsonObject =
        postGraphql(PROPERTY_QUERY, toApiPath(detailUrl))

    private suspend fun postGraphql(query: String, url: String): JsonObject {
        val body = buildJsonObject {
            put("query", query)
            put(
                "variables",
                buildJsonObject {
                    put("url", url)
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
        private fun toApiPath(detailUrl: String): String {
            return detailUrl
                .removePrefix("https://www.morizon.pl")
                .removePrefix("https://morizon.pl")
                .ifBlank { detailUrl }
        }

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
                    description
                    price { amount currency }
                    location {
                      location
                      street
                      map { center { latitude longitude } }
                    }
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

        private val PROPERTY_QUERY =
            """
            query getProperty(${'$'}url: String!) {
              getProperty(url: ${'$'}url) {
                id
                idOnFrontend
                title
                description
                url
                area
                numberOfRooms
                floorFormatted
                price { amount currency }
                location {
                  location
                  street
                  map { center { latitude longitude } }
                }
                photos { id name alt }
                contact {
                  person { name phones type }
                  company { name phones type }
                }
              }
            }
            """.trimIndent()
    }
}
