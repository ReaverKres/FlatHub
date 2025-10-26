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
