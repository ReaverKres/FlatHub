package kz.skiftrade.authdata.api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import server_request.Currency
import server_request.DealType
import server_request.Language
import server_response.KufarListResponse


interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @Query("cat")   categoryId: Int = 1010,
        @Query("cur")   currency: Currency = Currency.BYR,
        @Query("gtsy")  geoTag: String = "country-belarus~province-minsk~locality-minsk",
        @Query("lang")  language: Language = Language.RU,
        @Query("size")  pageSize: Int = 30,
        @Query("typ")   dealType: DealType = DealType.LET
    ): KufarListResponse
}
