package listing.ca.housesigma

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * HouseSigma map bbox + province. API supports ON, BC, AB only.
 * See tmp/ca/api/housesigma/NOTES.md.
 */
object HouseSigmaCities {
    data class Bbox(
        val lat1: Double,
        val lat2: Double,
        val lon1: Double,
        val lon2: Double,
    )

    data class Market(
        val province: String,
        val bbox: Bbox,
    )

    fun market(city: CityCode?): Market? = when (city) {
        CityCode.TORONTO -> Market(
            province = "ON",
            bbox = Bbox(43.58, 43.85, -79.64, -79.11),
        )

        CityCode.VANCOUVER -> Market(
            province = "BC",
            bbox = Bbox(49.19, 49.35, -123.25, -123.0),
        )

        CityCode.VICTORIA -> Market(
            province = "BC",
            bbox = Bbox(48.38, 48.52, -123.45, -123.25),
        )

        CityCode.CALGARY -> Market(
            province = "AB",
            bbox = Bbox(50.88, 51.18, -114.25, -113.85),
        )

        CityCode.EDMONTON -> Market(
            province = "AB",
            bbox = Bbox(53.45, 53.65, -113.65, -113.35),
        )

        CityCode.OTTAWA -> Market(
            province = "ON",
            bbox = Bbox(45.25, 45.45, -75.85, -75.55),
        )

        CityCode.HAMILTON -> Market(
            province = "ON",
            bbox = Bbox(43.18, 43.35, -80.05, -79.75),
        )

        else -> null
    }
}
