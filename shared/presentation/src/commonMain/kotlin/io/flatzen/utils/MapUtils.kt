package io.flatzen.utils

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

fun lonLatToNormalized(latitude: Double, longitude: Double): Pair<Double, Double> {
    val earthRadius = 6_378_137.0 // in meters
    val latRad = latitude * PI / 180.0
    val lngRad = longitude * PI / 180.0

    // Web Mercator projected coordiantes
    val X = earthRadius * lngRad
    val Y = earthRadius * ln(tan((PI / 4.0) + (latRad / 2.0)))

    // Relative coordinates for MapCompose
    val piR = earthRadius * PI
    val normalizedX = (X + piR) / (2.0 * piR)
    val normalizedY = (piR - Y) / (2.0 * piR)

    return Pair(normalizedX, normalizedY)
}

fun normalizedToLonLat(normalizedX: Double, normalizedY: Double): Pair<Double, Double> {
    val earthRadius = 6_378_137.0 // in meters
    val piR = earthRadius * PI

    // Convert normalized coordinates back to Web Mercator coordinates
    val X = normalizedX * (2.0 * piR) - piR
    val Y = piR - normalizedY * (2.0 * piR)

    // Convert Web Mercator coordinates back to longitude and latitude
    val longitude = X / earthRadius * 180.0 / PI

    // For latitude, we need to reverse the mercator projection formula
    val latRad = 2.0 * atan(exp(Y / earthRadius)) - PI / 2.0
    val latitude = latRad * 180.0 / PI

    return Pair(latitude, longitude)
}

/**
 * wmts level are 0 based.
 * At level 0, the map corresponds to just one tile.
 */
fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}