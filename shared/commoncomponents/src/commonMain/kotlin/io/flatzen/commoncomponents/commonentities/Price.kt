package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
data class Price(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
)

@Serializable
data class PriceInt(
    val priceFrom: Int? = null,
    val priceTo: Int? = null,
)

data class PriceText(
    val mainPrice: String?,
    val mainPerSquarePrice: String?,
    val localPrice: String?,
    val localPerSquarePrice: String?,
) {
    private val secondPriceStr = if (localPrice != null) {
        localPrice
    } else if (localPerSquarePrice != null) {
        localPerSquarePrice
    } else null

    private val mainPriceStr = if (mainPrice != null) {
        mainPrice
    } else if (mainPerSquarePrice != null) {
        mainPerSquarePrice
    } else null

    val mainPriceText =
        if (mainPriceStr == null && secondPriceStr != null) secondPriceStr else mainPriceStr
            ?: "Цена не указана"
    val secondPriceText =
        if (mainPriceStr == null && secondPriceStr != null) null else secondPriceStr
}
