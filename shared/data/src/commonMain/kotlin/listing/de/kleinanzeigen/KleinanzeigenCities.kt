package listing.de.kleinanzeigen

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Kleinanzeigen.de city location ids (c203 rent / c196 sale).
 */
object KleinanzeigenCities {
    data class CityLoc(val slug: String, val locationId: Int)

    fun location(city: CityCode?): CityLoc = when (city) {
        null, CityCode.BERLIN -> CityLoc("berlin", 3331)
        CityCode.MUENCHEN -> CityLoc("muenchen", 6411)
        CityCode.HAMBURG -> CityLoc("hamburg", 9409)
        CityCode.KOELN -> CityLoc("koeln", 945)
        CityCode.FRANKFURT -> CityLoc("frankfurt-am-main", 4292)
        CityCode.STUTTGART -> CityLoc("stuttgart", 9280)
        CityCode.DUESSELDORF -> CityLoc("duesseldorf", 2068)
        CityCode.LEIPZIG -> CityLoc("leipzig", 4233)
        else -> location(CityCode.BERLIN)
    }
}
