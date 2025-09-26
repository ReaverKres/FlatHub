package api

import core.NetworkResponseWrapper
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import server_request.Currency
import server_request.DomovitaRequest
import server_response.DomovitaListResponse

interface DomovitaApi {

    @POST("v1/objects/flats-rent/list")
    suspend fun searchRentFlats(
        @Body request: DomovitaRequest
    ): NetworkResponseWrapper<DomovitaListResponse>

    @POST("v1/objects/flats-sale/list")
    suspend fun searchSaleFlats(
        @Body request: DomovitaRequest
    ): NetworkResponseWrapper<DomovitaListResponse>

    companion object {
        fun createRequestParams(
            locationSefAlias: String = "minsk",
            page: Int = 1,
            limit: Int = 20,
            priceFull: Price? = null,
            pricePerSquare: Price? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null,
            sortOption: FlatSort = FlatSort.NEWEST_FIRST // Added sort option parameter
        ): DomovitaRequest {
            val priceMax = if (priceFull != null) {
                priceFull.priceTo
            } else pricePerSquare?.priceTo
            val priceMin = if (priceFull != null) {
                priceFull.priceFrom
            } else pricePerSquare?.priceFrom
            val priceParamName = if (priceFull != null) "price" else "price_m2"

            val offset: Int? = if (page >= 2) (page - 1) * limit else null
            
            // Map SortOption to Domovita sort parameter
            val domovitaSortParam = when (sortOption) {
                FlatSort.NEWEST_FIRST -> "created_at:desc"
                FlatSort.CHEAPEST_FIRST -> "price:asc"
                FlatSort.MOST_EXPENSIVE_FIRST -> "price:desc"
            }
            
            return DomovitaRequest(
                location = locationSefAlias,
                offset = offset,
                limit = limit,
                priceType = priceParamName,
                priceMax = priceMax,
                priceMin = priceMin,
                currency = Currency.USD.name,
                rooms = rooms?.toList(),
                metroStationIds = metroIds,
//                sort = domovitaSortParam, // Added sort parameter
//                isOwner = if (onlyOwner == true) "yes" else null
            )
        }
    }
}