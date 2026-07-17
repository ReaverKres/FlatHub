package listing.ae.opensooq

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * OpenSooq UAE city path segments: `/{locale}/{slug}/property/apartments-for-rent`.
 * See tmp/ae/api/opensooq/NOTES.md.
 */
object OpenSooqCities {
    fun slug(city: CityCode?): String = when (city) {
        null, CityCode.DUBAI -> "dubai"
        CityCode.ABU_DHABI -> "abu-dhabi"
        CityCode.SHARJAH -> "sharjah"
        CityCode.AJMAN -> "ajman"
        CityCode.AL_AIN -> "al-ain"
        CityCode.RAS_AL_KHAIMAH -> "ras-al-khaimah"
        CityCode.FUJAIRAH -> "fujairah"
        CityCode.UMM_AL_QUWAIN -> "umm-al-quwain"
        else -> "dubai"
    }
}
