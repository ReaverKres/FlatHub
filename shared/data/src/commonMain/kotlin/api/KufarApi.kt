package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.QueryMap
import server_response.kufar.KufarDailyListResponse
import server_response.kufar.KufarListResponse

interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @QueryMap queryParams: Map<String, String>,
        @Header("X-SearchID") searchId: String
    ): KufarListResponse

    @GET("booking/auth-bypass/v2/search")
    suspend fun searchFlatsDaily(
        @QueryMap queryParams: Map<String, String>,
    ): KufarDailyListResponse
}