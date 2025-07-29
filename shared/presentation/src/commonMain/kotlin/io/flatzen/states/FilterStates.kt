package io.flatzen.states

import androidx.compose.runtime.Immutable
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

enum class MetroLineState(val displayName: String) {
    GREEN("Зеленолужская линия"),
    BLUE("Московская линия"),
    RED("Автозаводская линия"),
}

enum class Room(val displayName: String) {
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
}

@Immutable
data class FilterState(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD,
    val rooms: Set<Int> = emptySet(),
    val metroLineState: List<MetroLineState> = emptyList(),
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