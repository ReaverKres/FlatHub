package io.flatzen.monetization.util

import kotlin.math.pow
import kotlin.math.roundToLong

actual fun <T : Number> T.roundToScale(scale: Int): T {
    val multiplier = 10.0.pow(scale)
    val rounded = (this.toDouble() * multiplier).roundToLong() / multiplier

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is Double -> rounded as T
        is Float -> rounded.toFloat() as T
        is Long -> rounded.toLong() as T
        is Int -> rounded.toInt() as T
        else -> rounded as T
    }
}
