package listing.ch.flatfox

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * City bbox seeds for Flatfox `/api/v1/pin/` (list `place=` is unreliable).
 * See tmp/ch/api/flatfox/NOTES.md.
 */
object FlatfoxCities {
    data class Bbox(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double,
    )

    fun bbox(city: CityCode?): Bbox = when (city) {
        CityCode.GENEVA -> Bbox(46.28, 46.13, 6.26, 6.03)
        CityCode.BASEL -> Bbox(47.63, 47.49, 7.71, 7.47)
        CityCode.BERN -> Bbox(47.02, 46.88, 7.57, 7.33)
        CityCode.LAUSANNE -> Bbox(46.59, 46.45, 6.75, 6.51)
        CityCode.WINTERTHUR -> Bbox(47.57, 47.43, 8.85, 8.60)
        CityCode.LUZERN -> Bbox(47.12, 46.98, 8.43, 8.19)
        CityCode.ST_GALLEN -> Bbox(47.50, 47.35, 9.50, 9.26)
        CityCode.LUGANO -> Bbox(46.08, 45.93, 9.07, 8.83)
        CityCode.BIEL -> Bbox(47.21, 47.06, 7.37, 7.13)
        else -> Bbox(47.45, 47.30, 8.65, 8.45) // Zürich default
    }

    fun placeSlug(city: CityCode?): String = when (city) {
        CityCode.GENEVA -> "geneva"
        CityCode.BASEL -> "basel"
        CityCode.BERN -> "bern"
        CityCode.LAUSANNE -> "lausanne"
        CityCode.WINTERTHUR -> "winterthur"
        CityCode.LUZERN -> "luzern"
        CityCode.ST_GALLEN -> "st-gallen"
        CityCode.LUGANO -> "lugano"
        CityCode.BIEL -> "biel"
        else -> "zurich"
    }
}
