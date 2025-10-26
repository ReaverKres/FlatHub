package server_response.kufar

import io.flatzen.commoncomponents.commonentities.FlatSort

object KufarDailyListQuery {
    private const val KUFAR_PAGE_SIZE = 30
    private const val KUFAR_MAX_PRICE = 1_000_000_000
    private const val KUFAR_MAX_COMMERCIAL_ROOMS = 20

    fun createQueryParams(
        categoryId: Int = 1010,
        currency: String = "USD",
        geoTag: String = "country-belarus~province-minsk~locality-minsk",
        language: String = "ru",
        cursor: String? = null,
        pageSize: Int = KUFAR_PAGE_SIZE,
        rooms: Set<Int>? = null,
        metroIds: List<Int>? = null,
        sortOption: FlatSort = FlatSort.NEWEST_FIRST,
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
            put("sort", kufarSortParam)
            put("size", pageSize.toString())
            if (!rooms.isNullOrEmpty()) {
                put("rms", "v.or:${rooms.joinToString(",")}")
            }
            if (!metroIds.isNullOrEmpty()) {
                put("mee", "v.or:${metroIds.joinToString(",")}")
            }
        }

        return params
    }
}
