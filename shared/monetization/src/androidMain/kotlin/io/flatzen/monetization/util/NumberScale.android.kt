package io.flatzen.monetization.util

import java.math.BigDecimal
import java.math.RoundingMode

actual fun <T : Number> T.roundToScale(scale: Int): T {
    val rounded = BigDecimal(this.toString())
        .setScale(scale, RoundingMode.HALF_UP)

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is Double -> rounded.toDouble() as T
        is Float -> rounded.toFloat() as T
        is Long -> rounded.toLong() as T
        is Int -> rounded.toInt() as T
        else -> rounded.toDouble() as T
    }
}
