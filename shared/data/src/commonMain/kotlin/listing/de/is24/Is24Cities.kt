package listing.de.is24

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * IS24 mobile API region geocode paths. See tmp/de/api/is24/NOTES.md.
 */
object Is24Cities {
    fun geocode(city: CityCode?): String = when (city) {
        null, CityCode.BERLIN -> "/de/berlin/berlin"
        CityCode.MUENCHEN -> "/de/bayern/muenchen"
        CityCode.HAMBURG -> "/de/hamburg/hamburg"
        CityCode.KOELN -> "/de/nordrhein-westfalen/koeln"
        CityCode.FRANKFURT -> "/de/hessen/frankfurt-am-main"
        CityCode.STUTTGART -> "/de/baden-wuerttemberg/stuttgart"
        CityCode.DUESSELDORF -> "/de/nordrhein-westfalen/duesseldorf"
        CityCode.LEIPZIG -> "/de/sachsen/leipzig"
        else -> geocode(CityCode.BERLIN)
    }
}
