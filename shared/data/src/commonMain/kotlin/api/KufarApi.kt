package api

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.QueryMap
import io.flatzen.commoncomponents.commonentities.AdType
import server_response.KufarListResponse

interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @QueryMap queryParams: Map<String, String>,
        @Header("X-SearchID") searchId: String
    ): KufarListResponse

    companion object {
        private const val KUFAR_PAGE_SIZE = 30

        fun createQueryParams(
            categoryId: Int = 1010,
            currency: String = "USD",
            geoTag: String = "country-belarus~province-minsk~locality-minsk",
            language: String = "ru",
            cursor: String? = null,
            pageSize: Int = KUFAR_PAGE_SIZE,
            dealType: AdType = AdType.RENT,
            sort: String = "lst.d",
            minPrice: Double? = null,
            maxPrice: Double? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null
        ): MutableMap<String, String> {
            val params = mutableMapOf<String, String>().apply {
                put("cat", categoryId.toString())
                put("cur", currency)
                put("gtsy", geoTag)
                put("lang", language)
                if (cursor.isNullOrBlank().not()) {
                    put("cursor", cursor.orEmpty())
                }
                put("size", pageSize.toString())
                if (dealType == AdType.RENT) {
                    put("typ", "let")
                } else {
                    put("typ", "sell")
                }
                put("sort", sort)
                if (onlyOwner != null && onlyOwner == true) {
                    put("cmp", "0")
                }
                if (!rooms.isNullOrEmpty()) {
                    put("rms", "v.or:${rooms.joinToString(",")}")
                }
                if (!metroIds.isNullOrEmpty()) {
                    put("mee", "v.or:${metroIds.joinToString(",")}")
                }
            }

            val priceParamName = if(dealType == AdType.RENT) "prc" else "psm"
            when {
                minPrice != null && maxPrice != null -> {
                    params[priceParamName] = "r:$minPrice,$maxPrice"
                }
                minPrice != null -> {
                    params[priceParamName] = "r:$minPrice"
                }
                maxPrice != null -> {
                    params[priceParamName] = "r:0,$maxPrice"
                }
            }

            return params
        }
    }
}
