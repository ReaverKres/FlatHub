package listing.at.is24

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * IS24 mobile API AT region geocodes. Same DE host; `/at/{bundesland}/{stadt}`.
 * See tmp/at/api/is24/NOTES.md.
 */
object Is24AtCities {
    fun geocode(city: CityCode?): String = when (city) {
        null, CityCode.WIEN -> "/at/wien/wien"
        CityCode.GRAZ -> "/at/steiermark/graz"
        CityCode.LINZ -> "/at/oberoesterreich/linz"
        CityCode.SALZBURG -> "/at/salzburg/salzburg"
        CityCode.INNSBRUCK -> "/at/tirol/innsbruck"
        CityCode.KLAGENFURT -> "/at/kaernten/klagenfurt-am-woerthersee"
        CityCode.VILLACH -> "/at/kaernten/villach"
        CityCode.WELS -> "/at/oberoesterreich/wels"
        CityCode.ST_POELTEN -> "/at/niederoesterreich/sankt-poelten"
        CityCode.DORNBIRN -> "/at/vorarlberg/dornbirn"
        else -> geocode(CityCode.WIEN)
    }
}
