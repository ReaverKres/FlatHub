package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import server_response.KufarListResponse


interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @Query("cat") categoryId: Int = 1010,
        @Query("cur") currency: String,
        @Query("gtsy") geoTag: String = "country-belarus~province-minsk~locality-minsk",
        @Query("lang") language: String,
        @Query("size") pageSize: Int = 30,
        @Query("typ") dealType: String,
        @Header("X-SearchID") searchId: String
    ): KufarListResponse
}
