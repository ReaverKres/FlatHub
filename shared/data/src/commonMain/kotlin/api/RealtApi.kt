package api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import server_response.RealtListResponse

interface RealtApi {
    @POST("bff/graphql")
    suspend fun searchFlats(
        @Body body: RealtGraphqlRequest
    ): RealtListResponse.RealtListResponseItem
}

@kotlinx.serialization.Serializable
data class RealtGraphqlRequest(
    val operationName: String,
    val variables: Variables,
    val query: String
) {
    companion object {
        val QUERY = """
          query searchObjects(${'$'}data: GetObjectsByAddressInput!) {
            searchObjects(data: ${'$'}data) {
              body {
                results {
                  uuid
                  code
                  title
                  price
                  priceCurrency
                  rooms
                  areaTotal
                  areaLiving
                  areaKitchen
                  storey
                  storeys
                  address
                  streetName
                  buildingNumber
                  metroStationName
                  metroLineId
                  createdAt
                  updatedAt
                  images
                  description
                  buildingYear
                  contactPhones
                  agencyName
                  contactName
                  contactEmail
                  location
                  isFavorite
                  paymentStatus
                  has3dTour
                  hasVideo
                }
                pagination {
                  page
                  pageSize
                }
                extraFields {
                  minPriceAggregation
                }
              }
              success
              errors {
                code
                title
                message
                field
              }
            }
          }
        """.trimIndent()
    }
}

@kotlinx.serialization.Serializable
data class Variables(
    val data: SearchData
)

@kotlinx.serialization.Serializable
data class SearchData(
    val where: Where,
    val pagination: PaginationRequestRealt,
    val sort: List<SortItem>,
    val extraFields: List<String>,
    val isReactAdaptiveUA: Boolean = false
)

@kotlinx.serialization.Serializable
data class Where(
    val addressV2: List<AddressV2>,
    val category: Int
)

@kotlinx.serialization.Serializable
data class AddressV2(
    val townUuid: String // Минск
)

@kotlinx.serialization.Serializable
data class PaginationRequestRealt(
    val page: Int,
    val pageSize: Int
)

@kotlinx.serialization.Serializable
data class SortItem(
    val by: String,
    val order: String
)