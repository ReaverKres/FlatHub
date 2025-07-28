package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap
import server_response.KufarListResponse
import server_response.OnlinerListResponse


interface OnlinerApi {

    @GET("sdapi/ak.api/search/apartments")
    suspend fun searchFlats(
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
            rooms: List<Int>? = null,
            metroLines: List<String>? = null
        ): Map<String, Any> {
            return mutableMapOf<String, Any>().apply {
                put("page", page)
                put("order", order)
                put("currency", currency)

                minPrice?.let { put("price[min]", it) }
                maxPrice?.let { put("price[max]", it) }

                boundsLbLat?.let { put("bounds[lb][lat]", it) }
                boundsLbLng?.let { put("bounds[lb][long]", it) }
                boundsRtLat?.let { put("bounds[rt][lat]", it) }
                boundsRtLng?.let { put("bounds[rt][long]", it) }

                rooms?.let {
                    it.forEach { room ->
                        put("number_of_rooms[]", room)
                    }
                }
                metroLines?.let {
                    it.forEach { line ->
                        put("metro[]", when (line.lowercase()) {
                            "red" -> "red_line"
                            "blue" -> "blue_line"
                            "green" -> "green_line"
                            else -> ""
                        })
                    }
                }
            }
        }
    }
}
