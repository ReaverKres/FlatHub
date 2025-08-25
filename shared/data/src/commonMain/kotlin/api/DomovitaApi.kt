package api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
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
            limit: Int = 30,
            minPrice: Double? = null,
            maxPrice: Double? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null
        ): DomovitaRequest {
            return DomovitaRequest(
                location = locationSefAlias,
                page = page,
                limit = limit,
                min_price_usd = minPrice?.toInt(),
                max_price_usd = maxPrice?.toInt(),
                rooms = rooms?.toList(),
                metro_ids = metroIds,
                owner_type = if (onlyOwner == true) "Собственник" else null
            )
        }
    }
}