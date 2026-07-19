package listing.us.zumper

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Zumper city path slugs: `/apartments-for-rent/{slug}`.
 * See tmp/us/api/zumper/NOTES.md.
 */
object UsCities {
    fun slug(city: CityCode?): String = when (city) {
        null, CityCode.NEW_YORK -> "new-york-ny"
        CityCode.LOS_ANGELES -> "los-angeles-ca"
        CityCode.CHICAGO -> "chicago-il"
        CityCode.HOUSTON -> "houston-tx"
        CityCode.MIAMI -> "miami-fl"
        CityCode.SEATTLE -> "seattle-wa"
        CityCode.SAN_FRANCISCO -> "san-francisco-ca"
        CityCode.AUSTIN -> "austin-tx"
        CityCode.BOSTON -> "boston-ma"
        CityCode.DENVER -> "denver-co"
        else -> "new-york-ny"
    }
}
