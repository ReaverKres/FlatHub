package api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import server_request.Currency
import server_request.DomovitaRequest
import server_response.DomovitaListResponse

interface DomovitaApi {

    @POST("v1/objects/flats-rent/list")
    suspend fun searchFlats(
        @Body request: DomovitaRequest
    ): DomovitaListResponse

    companion object {
        fun createRequestParams(
            locationSefAlias: String = "minsk",
            page: Int = 1,
            limit: Int = 20,
            minPrice: Double? = null,
            maxPrice: Double? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null
        ): DomovitaRequest {
            val offset: Int? = if (page >= 2) (page - 1) * limit else null
            return DomovitaRequest(
                location = locationSefAlias,
                offset = offset,
                limit = limit,
                priceType = "price",
                priceMax = maxPrice,
                priceMin = minPrice,
                currency = Currency.USD.name,
                rooms = rooms?.toList(),
                metroStationIds = metroIds,
//                isOwner = if (onlyOwner == true) "yes" else null
            )
        }
    }
}