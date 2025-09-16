package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
enum class FlatSort {
    NEWEST_FIRST,
    CHEAPEST_FIRST,
    MOST_EXPENSIVE_FIRST
}