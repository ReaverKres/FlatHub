package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
data class Price(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
)
