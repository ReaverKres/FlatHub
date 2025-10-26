package io.flatzen.commoncomponents.utils

import kotlin.math.roundToInt

private val intRegex = Regex("^(\\d+)")
private val priceRegex = Regex("(\\d)(?=(\\d{3})+$)")

val onlyDoublePredicate: (String) -> Boolean = { text ->
    val doubleRegex = Regex("^(\\d+)?[.,]?\\d{0,2}")
    text.isEmpty() || text.matches(doubleRegex)
}

val onlyIntPredicate: (String) -> Boolean = { text ->
    text.isEmpty() || text.matches(intRegex)
}

fun Double.asPriceFormat(): String {
    return this.roundToInt().toString().replace(priceRegex, "$1 ")
}

fun Double.asIntPrice(): String {
    return this.roundToInt().toString()
}

fun priceWithCurrency(price: Double?, currency: String): String {
    return price?.let {
        "${it.asPriceFormat()} $currency"
    } ?: "Цена не указана"
}

fun formatMainPrice(price: Double?, currency: String = "$"): String? {
    return if (price != null) {
        priceWithCurrency(price, currency)
    } else {
        null
    }
}

fun formatSecondPrice(price: Double?, isUsdPricePresent: Boolean): String? {
    return if (price != null) {
        val formatedPrice = priceWithCurrency(price, "BYN")
        if (!isUsdPricePresent) {
            formatedPrice
        } else {
            "($formatedPrice)"
        }
    } else {
        null
    }
}