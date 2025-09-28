package io.flatzen.utils

import io.flatzen.commoncomponents.commonentities.FlatSort

val onlyDoublePredicate: (String) -> Boolean = { text ->
    val doubleRegex = Regex("^(\\d+)?[.,]?\\d{0,2}")
    text.isEmpty() || text.matches(doubleRegex)
}

val FlatSort.text: String
    get() {
        return when (this) {
            FlatSort.NEWEST_FIRST -> "По новизне"
            FlatSort.CHEAPEST_FIRST -> "Сначала дешевле"
            FlatSort.MOST_EXPENSIVE_FIRST -> "Сначала дороже"

        }
    }