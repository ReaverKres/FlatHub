package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.QueryMap
import server_response.OnlinerListResponse


interface OnlinerApi {

    @GET("sdapi/ak.api/search/apartments")
    suspend fun searchRentFlats(
        @QueryMap params: Map<String, Any>,
        @Header("Accept") accept: String = "application/json"
    ): OnlinerListResponse

    @GET("sdapi/pk.api/search/apartments")
    suspend fun searchSaleFlats(
        @QueryMap params: Map<String, Any>,
        @Header("Accept") accept: String = "application/json"
    ): OnlinerListResponse

    companion object {
        fun createParams(
            page: Int = 1,
            order: String = "created_at:desc",
            minPrice: Int? = null,
            maxPrice: Int? = null,
            currency: String = "usd",
            boundsLbLat: Double? = null,
            boundsLbLng: Double? = null,
            boundsRtLat: Double? = null,
            boundsRtLng: Double? = null,
            rooms: Set<Int>? = null,
            metroLines: List<String>? = null,
            onlyOwner: Boolean? = null
        ): Map<String, Any> {
            return mutableMapOf<String, Any>().apply {
                put("page", page)
                put("order", order)
                put("currency", currency)
                if(onlyOwner != null && onlyOwner == true) {
                    put("only_owner", onlyOwner)
                }

                minPrice?.let { put("price[min]", it) }
                maxPrice?.let { put("price[max]", it) }

                boundsLbLat?.let { put("bounds[lb][lat]", it) }
                boundsLbLng?.let { put("bounds[lb][long]", it) }
                boundsRtLat?.let { put("bounds[rt][lat]", it) }
                boundsRtLng?.let { put("bounds[rt][long]", it) }

                rooms?.let {
                    it.forEach { room ->
                        put("rent_type[]", "${room}_rooms")
                    }
                }
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
