package api

import core.NetworkResponseWrapper
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap
import io.flatzen.commoncomponents.commonentities.FlatSort
import server_response.OnlinerListResponse


interface OnlinerApi {

    @GET("sdapi/ak.api/search/apartments")
    suspend fun searchRentFlats(
        @QueryMap params: Map<String, Any>,
        @Query("rent_type[]") rentTypes: List<String> = emptyList(),
        @Header("Accept") accept: String = "application/json"
    ): NetworkResponseWrapper<OnlinerListResponse>

    @GET("sdapi/pk.api/search/apartments")
    suspend fun searchSaleFlats(
        @QueryMap params: Map<String, Any>,
        @Query("number_of_rooms[]") numberOfRooms: List<Int> = emptyList(),
        @Header("Accept") accept: String = "application/json"
    ): NetworkResponseWrapper<OnlinerListResponse>

    companion object {
        fun createParams(
            page: Int = 1,
            order: String = "created_at:desc",
            minPrice: Double? = null,
            maxPrice: Double? = null,
            currency: String = "usd",
            boundsLbLat: Double? = null,
            boundsLbLng: Double? = null,
            boundsRtLat: Double? = null,
            boundsRtLng: Double? = null,
            metroLines: List<String>? = null,
            onlyOwner: Boolean? = null,
            sortOption: FlatSort = FlatSort.NEWEST_FIRST // Added sort option parameter
        ): Map<String, Any> {
            // Map SortOption to Onliner order parameter
            // У онлайнера нет сортировки?
            val onlinerOrderParam = when (sortOption) {
                FlatSort.NEWEST_FIRST -> "created_at:desc"
                FlatSort.CHEAPEST_FIRST -> "price:asc"
                FlatSort.MOST_EXPENSIVE_FIRST -> "price:desc"
            }

            return mutableMapOf<String, Any>().apply {
                put("page", page)
//                put("order", onlinerOrderParam) // Use the mapped sort parameter
                put("currency", currency)
                if(onlyOwner != null && onlyOwner) {
                    put("only_owner", onlyOwner)
                }

                minPrice?.let { put("price[min]", it) }
                maxPrice?.let { put("price[max]", it) }

                boundsLbLat?.let { put("bounds[lb][lat]", it) }
                boundsLbLng?.let { put("bounds[lb][long]", it) }
                boundsRtLat?.let { put("bounds[rt][lat]", it) }
                boundsRtLng?.let { put("bounds[rt][long]", it) }

                metroLines?.let {
                    it.forEach { line ->
                        put(
                            "metro[]", when (line.lowercase()) {
                                "red" -> "red_line"
                                "blue" -> "blue_line"
                                "green" -> "green_line"
                                else -> ""
                            }
                        )
                    }
                }
            }
        }
    }
}