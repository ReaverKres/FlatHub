package server_response.kufar

import entities.CommercialRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import mappers.kufar.KufarPropertyTypes

object KufarListQuery {

        private const val KUFAR_PAGE_SIZE = 30
        const val KUFAR_MAX_PRICE = 1_000_000_000
        private const val KUFAR_MAX_COMMERCIAL_ROOMS = 20

        fun createQueryParams(
            categoryId: Int = 1010,
            currency: String = "USD",
            geoTag: String = "country-belarus~province-minsk~locality-minsk",
            language: String = "ru",
            cursor: String? = null,
            pageSize: Int = KUFAR_PAGE_SIZE,
            dealType: AdType = AdType.RENT,
            priceFull: Price? = null,
            pricePerSquare: Price? = null,
            rooms: Set<Int>? = null,
            metroIds: List<Int>? = null,
            onlyOwner: Boolean? = null,
            sortOption: FlatSort = FlatSort.NEWEST_FIRST,
            commercialRequestModel: CommercialRequestModel? = null,
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
                if (dealType == AdType.RENT || dealType == AdType.COMMERCIAL(CommercialAdType.RENT)) {
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

            val fullPriceName = "prc"
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
                    params[squarePriceName] =
                        "r:${pricePerSquare.priceFrom},${pricePerSquare.priceTo}"
                }

                pricePerSquare?.priceFrom != null -> {
                    params[squarePriceName] = "r:${pricePerSquare.priceFrom},${KUFAR_MAX_PRICE}"
                }

                pricePerSquare?.priceTo != null -> {
                    params[squarePriceName] = "r:0,${pricePerSquare.priceTo}"
                }
            }

            val commercialRoomRange = commercialRequestModel?.roomRange
            val fullCommercialRoomsName = "cmrm"
            val intFromRange: Int? = commercialRoomRange?.fromRange?.toInt()
            val intToRange: Int? = commercialRoomRange?.toRange?.toInt()

            val commercialPropertyType = commercialRequestModel?.commercialPropertyType
            if (commercialPropertyType != null && KufarPropertyTypes.asParam(commercialPropertyType) != null){
                params["prt"] = KufarPropertyTypes.asParam(commercialPropertyType).orEmpty()
            }

            when {
                intFromRange != null && intToRange != null -> {
                    params[fullCommercialRoomsName] =
                        "r:${intFromRange},${intToRange}"
                }

                intFromRange != null -> {
                    params[fullCommercialRoomsName] =
                        "r:${intFromRange},${KUFAR_MAX_COMMERCIAL_ROOMS}"
                }

                intToRange != null -> {
                    params[fullCommercialRoomsName] = "r:0,${intToRange}"
                }
            }

            return params
        }
}