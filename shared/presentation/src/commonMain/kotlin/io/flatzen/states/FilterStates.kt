package io.flatzen.states

import androidx.compose.runtime.Immutable
import io.flatzen.mappers.LocationUiMapper.UiCityItem
import io.flatzen.mappers.MetroStationsMapper
import server_request.Currency

enum class RepairType(val displayName: String) {
    COSMETIC("Косметический"),
    EURO("Евроремонт"),
    DESIGNER("Дизайнерский"),
    NO_REPAIR("Без ремонта")
}

enum class Amenity(val displayName: String) {
    FURNITURE("Мебель"),
    NO_FURNITURE("Без мебели"),
    AIR_CONDITIONER("Кондиционер"),
    REFRIGERATOR("Холодильник"),
    WASHING_MACHINE("Стиральная машина"),
    DISHWASHER("Посудомойка"),
    INTERNET("Интернет"),
    TV("Телевизор"),
    BATHTUB("Ванна"),
    KITCHEN_FURNITURE("Кухонная мебель")
}

enum class MetroLineState() {
    GREEN,
    BLUE,
    RED,
}

@Immutable
data class UiMetroStation(
    val name: String,
    val line: MetroLineState,
    val selected: Boolean = false
)

enum class Room(val displayName: String) {
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
}

@Immutable
data class UiCountry(val code: String, val name: String? = null)

@Immutable
data class UiCity(val code: String, val name: String? = null)

@Immutable
data class LocationUiFilter(
    val selectedCountry: UiCountry = UiCountry("BY"),
    val selectedCity: UiCity = UiCity("MINSK"),
    val availableCities: List<UiCityItem> = listOf()
)

@Immutable
data class AddressUiState(
    val address: String
)

@Immutable
data class FilterState(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD,
    val rooms: Set<Int> = emptySet(),
    val metroStationsState: List<UiMetroStation> = MetroStationsMapper.allStationsOrderedForUi(),
    val location: LocationUiFilter? = null,
    val address: Set<AddressUiState>? = null,
    val fromOwnerOnly: Boolean = false,
    val kidsAllowed: Boolean = false,
    val petsAllowed: Boolean = false,
    val amenities: Set<Amenity> = emptySet(),
    val repairTypes: Set<RepairType> = emptySet(),
    val floorFrom: Int? = null,
    val floorTo: Int? = null,
    val totalAreaFrom: Double? = null,
    val totalAreaTo: Double? = null
)