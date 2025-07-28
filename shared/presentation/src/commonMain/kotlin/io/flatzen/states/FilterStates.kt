package io.flatzen.states

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import server_request.Currency

@Serializable
enum class RepairType(val displayName: String) {
    COSMETIC("Косметический"),
    EURO("Евроремонт"),
    DESIGNER("Дизайнерский"),
    NO_REPAIR("Без ремонта")
}

@Serializable
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

@Serializable
data class FilterState(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD,
    val rooms: Set<Int> = emptySet(),
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