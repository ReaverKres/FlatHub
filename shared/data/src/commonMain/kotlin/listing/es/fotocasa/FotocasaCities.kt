package listing.es.fotocasa

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Fotocasa gateway location params. See tmp/es/api/fotocasa/NOTES.md / cities.json.
 */
object FotocasaCities {
    data class CityLocation(
        val combinedLocationIds: String,
        val latitude: Double,
        val longitude: Double,
    )

    fun location(city: CityCode?): CityLocation = when (city) {
        null, CityCode.MADRID -> CityLocation(
            combinedLocationIds = "724,14,28,173,0,28079,0,0,0",
            latitude = 40.4096,
            longitude = -3.68624,
        )

        CityCode.BARCELONA -> CityLocation(
            combinedLocationIds = "724,9,8,232,376,8019,0,0,0",
            latitude = 41.3854,
            longitude = 2.17754,
        )

        CityCode.VALENCIA -> CityLocation(
            combinedLocationIds = "724,19,46,358,0,46250,0,0,0",
            latitude = 39.4766,
            longitude = -0.374407,
        )

        CityCode.SEVILLA -> CityLocation(
            combinedLocationIds = "724,1,41,328,0,41091,0,0,0",
            latitude = 37.3875,
            longitude = -5.99112,
        )

        CityCode.MALAGA -> CityLocation(
            combinedLocationIds = "724,1,29,319,547,29067,0,0,0",
            latitude = 36.7217,
            longitude = -4.41862,
        )

        CityCode.ZARAGOZA -> CityLocation(
            combinedLocationIds = "724,2,50,208,300,50297,0,0,0",
            latitude = 41.6576,
            longitude = -0.877996,
        )

        else -> location(CityCode.MADRID)
    }
}
