package api

import core.NetworkResponseWrapper
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.QueryMap
import entities.CommercialRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import mappers.kufar.KufarPropertyTypes
import server_response.kufar.KufarDailyListResponse
import server_response.kufar.KufarListResponse

interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @QueryMap queryParams: Map<String, String>,
        @Header("X-SearchID") searchId: String
    ): NetworkResponseWrapper<KufarListResponse>

    @GET("booking/auth-bypass/v2/search")
    suspend fun searchFlatsDaily(
        @QueryMap queryParams: Map<String, String>,
    ): NetworkResponseWrapper<KufarDailyListResponse>
}