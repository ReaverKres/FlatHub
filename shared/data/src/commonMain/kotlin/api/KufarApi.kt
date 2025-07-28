package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap
import entities.MetroLine
import server_response.KufarListResponse


interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @QueryMap queryParams: Map<String, String>,
        @Header("X-SearchID") searchId: String
    ): KufarListResponse

    companion object {
        fun createQueryParams(
            categoryId: Int = 1010,
            currency: String = "USD",
            geoTag: String = "country-belarus~province-minsk~locality-minsk",
            language: String = "ru",
            pageSize: Int = 30,
            dealType: String = "let",
            sort: String = "lst.d",
            minPrice: Double? = null,
            maxPrice: Double? = null,
            rooms: Int? = null,
            metroIds: List<Int>? = null
        ): MutableMap<String, String> {
            val params = mutableMapOf<String, String>().apply {
                put("cat", categoryId.toString())
                put("cur", currency)
                put("gtsy", geoTag)
                put("lang", language)
                put("size", pageSize.toString())
                put("typ", dealType)
                put("sort", sort)
                if (rooms != null) {
                    put("rms", "v.or:$rooms")
                }
                if (!metroIds.isNullOrEmpty()) {
                    put("mee", "v.or:${metroIds.joinToString(",")}")
                }
            }

            when {
                minPrice != null && maxPrice != null -> {
                    params["prc"] = "r:$minPrice,$maxPrice"
                }
                minPrice != null -> {
                    params["prc"] = "r:$minPrice"
                }
                maxPrice != null -> {
                    params["prc"] = "r:0,$maxPrice"
                }
            }

            return params
        }
    }
}
