package listing.ae.propertyfinder

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Property Finder location IDs (`l=` / filter locations_ids).
 * See tmp/ae/api/propertyfinder/NOTES.md.
 */
object PropertyFinderCities {
    fun locationId(city: CityCode?): Int = when (city) {
        null, CityCode.DUBAI -> 1
        CityCode.UMM_AL_QUWAIN -> 2
        CityCode.RAS_AL_KHAIMAH -> 3
        CityCode.SHARJAH -> 4
        CityCode.AJMAN -> 5
        CityCode.ABU_DHABI -> 6
        CityCode.FUJAIRAH -> 7
        CityCode.AL_AIN -> 8
        else -> 1
    }
}
