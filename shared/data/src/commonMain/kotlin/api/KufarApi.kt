package api

import core.NetworkResponseWrapper
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.QueryMap
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import server_response.KufarListResponse

interface KufarApi {

    @GET("search-api/v2/search/rendered-paginated")
    suspend fun searchFlats(
        @QueryMap queryParams: Map<String, String>,
        @Header("X-SearchID") searchId: String
    ): NetworkResponseWrapper<KufarListResponse>

    companion object {
        private const val KUFAR_PAGE_SIZE = 30
        private const val KUFAR_MAX_PRICE = 1_000_000_000

        fun createQueryParams(
            categoryId: Int = 1010,
            currency: String = "USD",
            geoTag: String = "country-belarus~province-minsk~locality-minsk",
            language: String = "ru",
            cursor: String? = null,
            pageSize: Int = KUFAR_PAGE_SIZE,
            dealType: AdType = AdType.RENT,
            sort: String = "lst.d",
            priceFull: Price? = null,
            pricePerSquare: Price? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null,
            sortOption: FlatSort = FlatSort.NEWEST_FIRST // Added sort option parameter
        ): MutableMap<String, String> {
            // Map SortOption to Kufar sort parameter
            val kufarSortParam = when (sortOption) {
                FlatSort.NEWEST_FIRST -> "lst.d"
                FlatSort.CHEAPEST_FIRST -> "prc.a"
                FlatSort.MOST_EXPENSIVE_FIRST -> "prc.d"
            }

            val params = mutableMapOf<String, String>().apply {
                put("cat", categoryId.toString())
                put("cur", currency)
                put("gtsy", geoTag)
                put("lang", language)
                if (cursor.isNullOrBlank().not()) {
                    put("cursor", cursor.orEmpty())
                }
                put("size", pageSize.toString())
                if (dealType == AdType.RENT || dealType == AdType.COMMERCIAL(CommercialType.RENT)) {
                    put("typ", "let")
                } else {
                    put("typ", "sell")
                }
                put("sort", kufarSortParam) // Use the mapped sort parameter
                if (onlyOwner != null && onlyOwner) {
                    put("cmp", "0")
                }
                if (!rooms.isNullOrEmpty()) {
                    put("rms", "v.or:${rooms.joinToString(",")}")
                }
                if (!metroIds.isNullOrEmpty()) {
                    put("mee", "v.or:${metroIds.joinToString(",")}")
                }
            }

            val fullPriceName =  "prc"
            val squarePriceName = "psm"

            when {
                priceFull?.priceFrom != null && priceFull.priceTo != null -> {
                    params[fullPriceName] = "r:${priceFull.priceFrom},${priceFull.priceTo}"
                }
                priceFull?.priceFrom != null -> {
                    params[fullPriceName] = "r:${priceFull.priceFrom},${KUFAR_MAX_PRICE}"
                }
                priceFull?.priceTo != null -> {
                    params[fullPriceName] = "r:0,${priceFull.priceTo}"
                }

                pricePerSquare?.priceFrom != null && pricePerSquare.priceTo != null -> {
                    params[squarePriceName] = "r:${pricePerSquare.priceFrom},${pricePerSquare.priceTo}"
                }
                pricePerSquare?.priceFrom != null -> {
                    params[squarePriceName] = "r:${pricePerSquare.priceFrom},${KUFAR_MAX_PRICE}"
                }
                pricePerSquare?.priceTo != null -> {
                    params[squarePriceName] = "r:0,${pricePerSquare.priceTo}"
                }
            }

            return params
        }
    }
}