package listing.ae.dubizzle

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Dubizzle Algolia `city.id` values.
 * See tmp/ae/api/dubizzle/NOTES.md.
 */
object DubizzleCities {
    fun cityId(city: CityCode?): Int = when (city) {
        null, CityCode.DUBAI -> 2
        CityCode.ABU_DHABI -> 3
        CityCode.RAS_AL_KHAIMAH -> 11
        CityCode.SHARJAH -> 12
        CityCode.FUJAIRAH -> 13
        CityCode.AJMAN -> 14
        CityCode.UMM_AL_QUWAIN -> 15
        CityCode.AL_AIN -> 39
        else -> 2
    }
}
