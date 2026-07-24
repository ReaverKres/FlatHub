package listing.ca.zolo

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Zolo.ca city URL slugs: `/{slug}-real-estate/for-rent|for-sale`.
 * Montreal has no local inventory — use Centris for QC.
 * See tmp/ca/api/zolo/NOTES.md.
 */
object ZoloCities {
    fun slug(city: CityCode?): String? = when (city) {
        CityCode.TORONTO -> "toronto"
        CityCode.VANCOUVER -> "vancouver"
        CityCode.OTTAWA -> "ottawa"
        CityCode.CALGARY -> "calgary"
        CityCode.EDMONTON -> "edmonton"
        CityCode.HAMILTON -> "hamilton"
        CityCode.WINNIPEG -> "winnipeg"
        CityCode.VICTORIA -> "victoria"
        CityCode.MONTREAL, CityCode.QUEBEC_CITY -> null
        else -> null
    }
}
