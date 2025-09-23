package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
data class FromToRange(
    val fromRange: Double? = null,
    val toRange: Double? = null,
)
