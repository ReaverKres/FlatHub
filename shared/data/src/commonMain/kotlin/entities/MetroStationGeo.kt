package entities

import io.flatzen.commoncomponents.commonentities.Coordinates

/**
 * Geo-enriched metro station with canonical filter name and line.
 */
data class MetroStationGeo(
    val jsonName: String,
    val canonicalName: String,
    val line: MetroLine,
    val coordinates: Coordinates,
    val metroId: Int,
)
