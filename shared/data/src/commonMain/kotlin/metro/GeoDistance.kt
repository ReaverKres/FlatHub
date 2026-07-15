package metro

import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun distanceMeters(from: Coordinates, to: Coordinates): Double {
    val dLat = degToRad(to.latitude - from.latitude)
    val dLon = degToRad(to.longitude - from.longitude)
    val lat1 = degToRad(from.latitude)
    val lat2 = degToRad(to.latitude)
    val a = sin(dLat / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

private fun degToRad(degrees: Double): Double = degrees * PI / 180.0
