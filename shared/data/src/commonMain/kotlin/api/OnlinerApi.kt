package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import server_response.KufarListResponse
import server_response.OnlinerListResponse


interface OnlinerApi {

    @GET("sdapi/ak.api/search/apartments")
    suspend fun searchFlats(
        @Query("page") page: Int = 1,
        @Query("order") order: String = "created_at:desc", // сначала новые
        @Query("bounds[lb][lat]") boundsLbLat: Double? = null,
        @Query("bounds[lb][long]") boundsLbLng: Double? = null,
        @Query("bounds[rt][lat]") boundsRtLat: Double? = null,
        @Query("bounds[rt][long]") boundsRtLng: Double? = null
    ): OnlinerListResponse
}
