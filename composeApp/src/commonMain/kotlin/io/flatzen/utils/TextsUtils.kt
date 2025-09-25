package io.flatzen.utils

val onlyDoublePredicate: (String) -> Boolean = { text ->
    val doubleRegex = Regex("^(\\d+)?[.,]?\\d{0,2}")
    text.isEmpty() || text.matches(doubleRegex)
}