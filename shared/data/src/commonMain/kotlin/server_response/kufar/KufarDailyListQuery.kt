package server_response.kufar

import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.PriceInt

object KufarDailyListQuery {
    private const val KUFAR_PAGE_SIZE = 30
    private const val KUFAR_MAX_PRICE = 1_000_000_000
    private const val KUFAR_MAX_COMMERCIAL_ROOMS = 20

    fun createQueryParams(
        categoryId: Int = 1010,
        geoTag: String = "country-belarus~province-minsk~locality-minsk",
        language: String = "ru",
        cursor: String? = null,
        priceFull: Price? = null,
        pageSize: Int = KUFAR_PAGE_SIZE,
        rooms: Set<Int>? = null,
        metroIds: List<Int>? = null,
        sortOption: FlatSort = FlatSort.NEWEST_FIRST,
    ): MutableMap<String, String> {
        val updatedPriceFull: PriceInt? = priceFull?.copy(
            priceFrom = priceFull.priceFrom?.times(100),
            priceTo = priceFull.priceTo?.times(100)
        ).let {
            PriceInt(
                priceFrom = it?.priceFrom?.toInt(),
                priceTo = it?.priceTo?.toInt()
            )
        }
        // Map SortOption to Kufar sort parameter
        val kufarSortParam = when (sortOption) {
            FlatSort.NEWEST_FIRST -> "lst.d"
            FlatSort.CHEAPEST_FIRST -> "prc.a"
            FlatSort.MOST_EXPENSIVE_FIRST -> "prc.d"
        }

        val params = mutableMapOf<String, String>().apply {
            put("cat", categoryId.toString())
            put("gtsy", geoTag)
            put("lang", language)
            if (cursor.isNullOrBlank().not()) {
                put("cursor", cursor.orEmpty())
            }
            put("sort", kufarSortParam)
            put("size", pageSize.toString())
            if (!rooms.isNullOrEmpty()) {
                put("rms", "v.or:${rooms.joinToString(",")}")
            }
            if (!metroIds.isNullOrEmpty()) {
                put("mee", "v.or:${metroIds.joinToString(",")}")
            }
        }

        val fullPriceName = "prc"
        when {
            updatedPriceFull?.priceFrom != null && updatedPriceFull.priceTo != null -> {
                params[fullPriceName] =
                    "r:${updatedPriceFull.priceFrom},${updatedPriceFull.priceTo}"
            }

            updatedPriceFull?.priceFrom != null -> {
                params[fullPriceName] =
                    "r:${updatedPriceFull.priceFrom},${KufarListQuery.KUFAR_MAX_PRICE}"
            }

            updatedPriceFull?.priceTo != null -> {
                params[fullPriceName] = "r:0,${updatedPriceFull.priceTo}"
            }
        }

        return params
    }
}
