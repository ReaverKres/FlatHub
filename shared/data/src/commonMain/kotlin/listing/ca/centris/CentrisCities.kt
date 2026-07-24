package listing.ca.centris

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Centris.ca region path slugs (Quebec MLS only).
 * See tmp/ca/api/centris/NOTES.md.
 */
object CentrisCities {
    fun regionSlug(city: CityCode?): String? = when (city) {
        CityCode.MONTREAL -> "montreal-island"
        CityCode.QUEBEC_CITY -> "quebec"
        else -> null
    }
}
