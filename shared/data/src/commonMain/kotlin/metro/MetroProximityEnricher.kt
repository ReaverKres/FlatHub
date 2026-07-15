package metro

import entities.AppFlat
import entities.MetroLine

/**
 * Fills [AppFlat.metroStation] for flats without metro when coordinates
 * are within 1 km of a known Minsk metro station.
 */
object MetroProximityEnricher {
    private const val RADIUS_METERS = 1_000.0

    fun enrich(
        flat: AppFlat,
        preferredLines: Set<MetroLine> = emptySet(),
    ): AppFlat {
        if (!flat.metroStation.isNullOrBlank()) return flat
        val coordinates = flat.coordinates ?: return flat
        val nearest = MetroStationsGeoCatalog.findNearestWithinMeters(
            coordinates = coordinates,
            maxMeters = RADIUS_METERS,
            preferredLines = preferredLines,
        ) ?: return flat
        return flat.copy(metroStation = nearest.canonicalName)
    }
}
