package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.UserMapArea
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.AdType.COMMERCIAL
import io.flatzen.commoncomponents.commonentities.AdType.DAILY
import io.flatzen.commoncomponents.commonentities.AdType.RENT
import io.flatzen.commoncomponents.commonentities.AdType.SALE
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.LocationUiMapper.UiCityItem
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.viewmodel.UiDistrict
import server_request.Currency

enum class MetroLineState() {
    GREEN, BLUE, RED,
}

@Immutable
data class UiMetroStation(
    val name: String, val line: MetroLineState, val selected: Boolean = false
)

enum class Room(val displayName: String) {
    ONE("1"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"),
}

@Immutable
data class UiCountry(val code: CountryCode, val name: String? = null)

@Immutable
data class LocationUiFilter(
    val selectedCountry: UiCountry = UiCountry(CountryCode.BY),
    val selectedCity: UiCityItem = UiCityItem(
        CityCode.MINSK, "Минск", Coordinates(53.902147, 27.561388)
    ),
    val availableCities: List<UiCityItem> = LocationUiMapper.cities()
)

@Immutable
data class AddressUiState(
    val address: String
)

@Immutable
data class SaveDialogState(
    val isVisible: Boolean = false,
    val filterName: String = "",
    val isNameValid: Boolean = true,
    val errorMessage: String? = null,
    val showNotification: Boolean = false
)

@Immutable
data class SavedFilterState(
    val id: Long, val name: String, val selected: Boolean = false, val createdAt: Long
)

@Immutable
data class MapAreasUi(
    val id: String,
    val coordinates: List<Coordinates>,
    val isActive: Boolean,
    val name: String
) {
    companion object {
        fun mapFromModelToUi(areas: List<UserMapArea>): List<MapAreasUi> {
            return areas.map {
                MapAreasUi(
                    id = it.pathId,
                    coordinates = it.coordinates,
                    isActive = it.isActive,
                    name = it.name
                )
            }
        }

        fun mapFromUiToModel(areas: List<MapAreasUi>?): List<UserMapArea> {
            return areas?.map {
                UserMapArea(
                    pathId = it.id,
                    coordinates = it.coordinates,
                    isActive = it.isActive,
                    name = it.name
                )
            } ?: emptyList()
        }
    }
}

@Immutable
data class FilterState(
    val adType: AdType = RENT,
    val lastCommercialAdType: AdType = COMMERCIAL(),
    val priceFull: Price? = null,
    val pricePerSquare: Price? = null,
    val totalArea: FromToRange? = null,
    val currency: Currency = Currency.USD,
    val rooms: Set<Int> = emptySet(),
    val metroStationsState: List<UiMetroStation> = MetroStationsMapper.allStationsOrderedForUi(),
    val location: LocationUiFilter? = null,
    val userMapAreas: List<MapAreasUi>? = null,
    val districtsArea: List<UiDistrict>? = null,
    val address: Set<AddressUiState>? = null,
    val fromOwnerOnly: Boolean = false,
    val withPhotoOnly: Boolean = false,
    val roomOnly: Boolean = false,
    val sortOption: FlatSort = FlatSort.NEWEST_FIRST, // Added sort option
    val commercial: CommercialFilters = CommercialFilters(),
    val bookingDatesFilter: BookingDatesFilter? = null,
    val isNotificationEnabled: Boolean = false,
) {
    fun isLocationFilterActive(): Boolean {
        return address.isNullOrEmpty().not() || metroStationsState.any { it.selected }
    }

    fun getSelectedMetroStation(): String =
        metroStationsState.filter { it.selected }.joinToString(separator = ", ") { it.name }

    fun getSelectedAddress(): String =
        address?.joinToString(separator = ", ") { it.address }.orEmpty()

    fun getActiveFiltersText(): String {
        val activeFilters = mutableListOf<String>()

        // Тип объявления
        activeFilters.add("Тип: ${when (adType) {
            RENT -> "Аренда"
            SALE -> "Продажа"
            COMMERCIAL(CommercialAdType.SALE) -> "Коммерческая (купить)"
            COMMERCIAL(CommercialAdType.RENT) -> "Коммерческая (снять)"
            is COMMERCIAL -> "Коммерческая"
            is DAILY -> "Посуточно"
        }}")

        if(adType.isCommercial) {
            commercial.commercialPropertyType?.find { it.selected }?.let { type ->
                activeFilters.add("Тип помещения: ${type.commercialPropertyTypeName}")
            }
        }

        // Полная цена
        priceFull?.let { price ->
            activeFilters.add("Цена: ${price.priceFrom?.let { "от $it" } ?: ""} ${price.priceTo?.let { "до $it" } ?: ""} ${
                when (currency) {
                    Currency.USD -> "$"
                    Currency.EUR -> "€"
                    else -> currency.toString()
                }
            }".trim())
        }

        // Цена за м²
        pricePerSquare?.let { price ->
            activeFilters.add("Цена за м²: ${price.priceFrom?.let { "от $it" } ?: ""} ${price.priceTo?.let { "до $it" } ?: ""} ${
                when (currency) {
                    Currency.USD -> "$"
                    Currency.EUR -> "€"
                    else -> currency.toString()
                }
            }".trim())
        }

        // Общая площадь
        totalArea?.let { area ->
            activeFilters.add("Общая площадь: ${area.fromRange?.let { "от $it" } ?: ""} ${area.toRange?.let { "до $it" } ?: ""} м²".trim())
        }

        // Комнаты
        if (rooms.isNotEmpty()) {
            val roomsText = rooms.sorted().joinToString(", ") {
                when (it) {
                    0 -> "студия"
                    5 -> "5+ комнат"
                    else -> "$it ${getRoomWord(it)}"
                }
            }
            activeFilters.add("Комнаты: $roomsText")
        }

        // Метро
        val selectedMetro = metroStationsState.filter { it.selected }
        if (selectedMetro.isNotEmpty()) {
            val metroText = selectedMetro.joinToString(", ") { it.name }
            activeFilters.add("Метро: $metroText")
        }

        // Адрес
        if (!address.isNullOrEmpty()) {
            val addressText = address.joinToString(", ") { it.address }
            activeFilters.add("Адрес: $addressText")
        }

        // Локация
        location?.let {
            activeFilters.add("Локация: ${it.selectedCity.displayName}")
        }
        val activeAreas = userMapAreas?.filter { it.isActive }
        if(activeAreas.isNullOrEmpty().not()) {
            val areasText = activeAreas.joinToString(", ") { it.name }
            activeFilters.add("Активные области: $areasText")
        }
        val activeDistricts = districtsArea?.filter { it.isChecked }
        if(activeDistricts.isNullOrEmpty().not()) {
            val districtsText = activeDistricts.joinToString(", ") { it.nameLocal }
            activeFilters.add("Районы: $districtsText")
        }

        // Булевы фильтры
        if (fromOwnerOnly) activeFilters.add("Только от собственника")
        if (withPhotoOnly) activeFilters.add("Только с фото")
        if (roomOnly) activeFilters.add("Только комната")

//        activeFilters.add("Сортировка: ${getSortOptionName(sortOption)}")

        return if (activeFilters.isEmpty()) {
            "Активные фильтры не выбраны"
        } else {
            "Активные фильтры:\n" + activeFilters.joinToString("\n• ", "• ")
        }
    }

    // Вспомогательные функции для правильного склонения и перевода
    private fun getRoomWord(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "комната"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "комнаты"
            else -> "комнат"
        }
    }

    private fun getSortOptionName(sortOption: FlatSort): String {
        return when (sortOption) {
            FlatSort.NEWEST_FIRST -> "сначала новые"
            FlatSort.CHEAPEST_FIRST -> "сначала дешёвые"
            FlatSort.MOST_EXPENSIVE_FIRST -> "сначала дорогие"
        }
    }
}