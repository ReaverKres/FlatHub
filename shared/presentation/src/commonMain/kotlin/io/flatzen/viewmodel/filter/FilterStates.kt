package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.UserMapArea
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.AdType.COMMERCIAL
import io.flatzen.commoncomponents.commonentities.AdType.DAILY
import io.flatzen.commoncomponents.commonentities.AdType.JEONSE
import io.flatzen.commoncomponents.commonentities.AdType.RENT
import io.flatzen.commoncomponents.commonentities.AdType.SALE
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.commonentities.usesSquareFeet
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.commoncomponents.location.networkCountryIso
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.LocationUiMapper.UiCityItem
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.viewmodel.UiDistrict
import server_request.Currency
import server_request.filterCurrency

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
    val selectedCountry: UiCountry,
    val selectedCity: UiCityItem,
    val availableCities: List<UiCityItem>,
) {
    companion object {
        fun networkDefault(): LocationUiFilter {
            val country = CountryCode.fromNetworkIso(networkCountryIso())
            val city = LocationUiMapper.defaultCity(country)
            return LocationUiFilter(
                selectedCountry = UiCountry(
                    code = country,
                    name = LocationUiMapper.countryDisplayName(country),
                ),
                selectedCity = city,
                availableCities = LocationUiMapper.cities(country),
            )
        }
    }
}

@Immutable
data class AddressUiState(
    val address: String
)

@Immutable
data class SaveDialogState(
    val isVisible: Boolean = false,
    val filterName: String = "",
    val isNameValid: Boolean = true,
    val errorMessage: LocalizationKeys? = null,
    val isNotificationEnabled: Boolean = false,
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
    val name: String? = null,
    val adType: AdType = RENT,
    val lastCommercialAdType: AdType = COMMERCIAL(),
    val priceFull: Price? = null,
    val pricePerSquare: Price? = null,
    val totalArea: FromToRange? = null,
    val currency: Currency = run {
        val country = LocationUiFilter.networkDefault().selectedCountry?.code
            ?: CountryCode.BY
        country.filterCurrency(RENT)
    },
    val rooms: Set<Int> = emptySet(),
    val metroStationsState: List<UiMetroStation> = MetroStationsMapper.allStationsOrderedForUi(),
    val withAnyMetro: Boolean = false,
    val location: LocationUiFilter? = LocationUiFilter.networkDefault(),
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
        return address.isNullOrEmpty().not() ||
                withAnyMetro ||
                metroStationsState.any { it.selected }
    }

    fun hasPaidLocationFilters(): Boolean =
        withAnyMetro ||
                metroStationsState.any { it.selected } ||
                !address.isNullOrEmpty() ||
                districtsArea?.any { it.isChecked } == true ||
                userMapAreas?.any { it.isActive } == true

    fun stripPaidLocationFilters(): FilterState = copy(
        withAnyMetro = false,
        metroStationsState = metroStationsState.map { it.copy(selected = false) },
        address = null,
        districtsArea = districtsArea?.map { it.copy(isChecked = false) },
        userMapAreas = userMapAreas?.map { it.copy(isActive = false) },
    )

    fun getSelectedMetroStation(): String =
        metroStationsState.filter { it.selected }.joinToString(separator = ", ") { it.name }

    fun getSelectedAddress(): String =
        address?.joinToString(separator = ", ") { it.address }.orEmpty()

    fun getActiveFiltersText(): String = getActiveFiltersText(::defaultRussianString)

    fun getActiveFiltersText(resolve: (LocalizationKeys) -> String): String {
        val activeFilters = mutableListOf<String>()
        val fromText = resolve(LocalizationKeys.FROM).lowercase()
        val toText = resolve(LocalizationKeys.TO).lowercase()

        // Тип объявления
        activeFilters.add("${resolve(LocalizationKeys.FILTER_TYPE_PREFIX)}: ${when (adType) {
            RENT -> resolve(LocalizationKeys.FILTER_RENT)
            SALE -> resolve(LocalizationKeys.FILTER_SALE)
            COMMERCIAL(CommercialAdType.SALE) -> resolve(LocalizationKeys.FILTER_COMMERCIAL_SALE)
            COMMERCIAL(CommercialAdType.RENT) -> resolve(LocalizationKeys.FILTER_COMMERCIAL_RENT)
            is COMMERCIAL -> resolve(LocalizationKeys.FILTER_COMMERCIAL)
            is DAILY -> resolve(LocalizationKeys.FILTER_DAILY)
            is JEONSE -> resolve(LocalizationKeys.FILTER_JEONSE)
        }}")

        if (adType.isCommercial) {
            commercial.commercialPropertyType?.find { it.selected }?.let { type ->
                type.commercialPropertyTypeName?.let { key ->
                    activeFilters.add(
                        "${resolve(LocalizationKeys.FILTER_PROPERTY_TYPE)}: ${
                            resolve(
                                key
                            )
                        }"
                    )
                }
            }
        }

        // Полная цена
        priceFull?.let { price ->
            activeFilters.add("${resolve(LocalizationKeys.FILTER_PRICE_LABEL)}: ${price.priceFrom?.let { "$fromText $it" } ?: ""} ${price.priceTo?.let { "$toText $it" } ?: ""} ${
                currency.filterLabel()
            }".trim())
        }

        // Цена за м² / sqft
        pricePerSquare?.let { price ->
            val pricePerSquareLabel =
                if (location?.selectedCountry?.code?.usesSquareFeet() == true) {
                    resolve(LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL_SQFT)
                } else {
                    resolve(LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL)
                }
            activeFilters.add("$pricePerSquareLabel: ${price.priceFrom?.let { "$fromText $it" } ?: ""} ${price.priceTo?.let { "$toText $it" } ?: ""} ${
                currency.filterLabel()
            }".trim())
        }

        // Общая площадь
        totalArea?.let { area ->
            val areaUnit =
                if (location?.selectedCountry?.code?.usesSquareFeet() == true) "sqft" else "м²"
            activeFilters.add("${resolve(LocalizationKeys.DETAIL_TOTAL_AREA)}: ${area.fromRange?.let { "$fromText $it" } ?: ""} ${area.toRange?.let { "$toText $it" } ?: ""} $areaUnit".trim())
        }

        // Комнаты
        if (rooms.isNotEmpty()) {
            val roomsText = rooms.sorted().joinToString(", ") {
                when (it) {
                    0 -> "1 ${getRoomWord(1)}"
                    5 -> "5+ комнат"
                    else -> "$it ${getRoomWord(it)}"
                }
            }
            activeFilters.add("${resolve(LocalizationKeys.DETAIL_ROOMS_COUNT)}: $roomsText")
        }

        // Метро
        if (withAnyMetro) {
            activeFilters.add(
                "${resolve(LocalizationKeys.FILTER_METRO_PREFIX)}: ${resolve(LocalizationKeys.FILTER_METRO_ANY)}"
            )
        } else {
            val selectedMetro = metroStationsState.filter { it.selected }
            if (selectedMetro.isNotEmpty()) {
                val metroText = selectedMetro.joinToString(", ") { it.name }
                activeFilters.add("${resolve(LocalizationKeys.FILTER_METRO_PREFIX)}: $metroText")
            }
        }

        // Адрес
        if (!address.isNullOrEmpty()) {
            val addressText = address.joinToString(", ") { it.address }
            activeFilters.add("${resolve(LocalizationKeys.FILTER_ADDRESS_PREFIX)}: $addressText")
        }

        // Локация
        location?.let {
            activeFilters.add("${resolve(LocalizationKeys.FILTER_CITY_PREFIX)}: ${it.selectedCity.displayName}")
        }
        val activeAreas = userMapAreas?.filter { it.isActive }
        if(activeAreas.isNullOrEmpty().not()) {
            val areasText = activeAreas.joinToString(", ") { it.name }
            activeFilters.add("${resolve(LocalizationKeys.FILTER_ACTIVE_AREAS_PREFIX)}: $areasText")
        }
        val activeDistricts = districtsArea?.filter { it.isChecked }
        if(activeDistricts.isNullOrEmpty().not()) {
            val districtsText = activeDistricts.joinToString(", ") { it.nameLocal }
            activeFilters.add("${resolve(LocalizationKeys.FILTER_DISTRICTS_PREFIX)}: $districtsText")
        }

        // Булевы фильтры
        if (fromOwnerOnly) activeFilters.add(resolve(LocalizationKeys.FILTER_OWNER_ONLY))
        if (withPhotoOnly) activeFilters.add(resolve(LocalizationKeys.FILTER_PHOTO_ONLY))
        if (roomOnly) activeFilters.add(resolve(LocalizationKeys.FILTER_ROOM_ONLY))

//        activeFilters.add("Сортировка: ${getSortOptionName(sortOption)}")

        return if (activeFilters.isEmpty()) {
            resolve(LocalizationKeys.FILTER_ACTIVE_NONE)
        } else {
            "${resolve(LocalizationKeys.FILTER_ACTIVE_TITLE)}\n" + activeFilters.joinToString("\n• ", "• ")
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
}

private fun defaultRussianString(key: LocalizationKeys): String {
    return when (key) {
        LocalizationKeys.FILTER_RENT -> "Аренда"
        LocalizationKeys.FILTER_SALE -> "Продажа"
        LocalizationKeys.FILTER_COMMERCIAL_SALE -> "Коммерческая (Купить)"
        LocalizationKeys.FILTER_COMMERCIAL_RENT -> "Коммерческая (Снять)"
        LocalizationKeys.FILTER_COMMERCIAL -> "Коммерческая"
        LocalizationKeys.FILTER_DAILY -> "Посуточно"
        LocalizationKeys.FILTER_JEONSE -> "Чонсе"
        LocalizationKeys.FILTER_PROPERTY_TYPE -> "Тип помещения"
        LocalizationKeys.FILTER_PRICE_LABEL -> "Цена"
        LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL -> "Цена за м²"
        LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL_SQFT -> "Цена за sqft"
        LocalizationKeys.DETAIL_TOTAL_AREA -> "Общая площадь"
        LocalizationKeys.DETAIL_ROOMS_COUNT -> "Количество комнат"
        LocalizationKeys.FILTER_METRO_PREFIX -> "Метро"
        LocalizationKeys.FILTER_METRO_ANY -> "Любая станция"
        LocalizationKeys.FILTER_ADDRESS_PREFIX -> "Адрес"
        LocalizationKeys.LOCATION_METRO_LINE_RED -> "Красная"
        LocalizationKeys.LOCATION_METRO_LINE_BLUE -> "Синяя"
        LocalizationKeys.LOCATION_METRO_LINE_GREEN -> "Зелёная"
        LocalizationKeys.LOCATION_METRO_ANY_SWITCH -> "Любая станция"
        LocalizationKeys.FILTER_CITY_PREFIX -> "Локация"
        LocalizationKeys.FILTER_ACTIVE_AREAS_PREFIX -> "Активные области"
        LocalizationKeys.FILTER_DISTRICTS_PREFIX -> "Районы"
        LocalizationKeys.FILTER_OWNER_ONLY -> "Только от собственника"
        LocalizationKeys.FILTER_PHOTO_ONLY -> "Только с фото"
        LocalizationKeys.FILTER_ROOM_ONLY -> "Только комната"
        LocalizationKeys.FILTER_ACTIVE_NONE -> "Активные фильтры не выбраны"
        LocalizationKeys.FILTER_ACTIVE_TITLE -> "Активные фильтры:"
        LocalizationKeys.FILTER_TYPE_PREFIX -> "Тип"
        LocalizationKeys.FROM -> "От"
        LocalizationKeys.TO -> "До"
        LocalizationKeys.COMMERCIAL_PROPERTY_ALL -> "Все"
        LocalizationKeys.COMMERCIAL_PROPERTY_INDUSTRIAL -> "Промышленные помещения"
        LocalizationKeys.COMMERCIAL_PROPERTY_OFFICE -> "Офис"
        LocalizationKeys.COMMERCIAL_PROPERTY_RETAIL -> "Торговые помещения"
        LocalizationKeys.COMMERCIAL_PROPERTY_SERVICES -> "Сфера услуг"
        LocalizationKeys.COMMERCIAL_PROPERTY_WAREHOUSES -> "Склады"
        LocalizationKeys.COMMERCIAL_PROPERTY_OTHER -> "Прочая коммерческая"
        else -> key.name
    }
}