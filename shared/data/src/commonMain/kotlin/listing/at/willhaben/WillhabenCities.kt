package listing.at.willhaben

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * willhaben.at areaId for apartment search. See tmp/at/api/willhaben/areas.json.
 * Only NOTES-verified IDs; other AT MVP cities fall back to Wien until device probe.
 */
object WillhabenCities {
    fun areaId(city: CityCode?): Int = when (city) {
        null, CityCode.WIEN -> 900
        CityCode.GRAZ -> 60101
        CityCode.LINZ -> 40101
        CityCode.SALZBURG -> 50101
        CityCode.INNSBRUCK -> 70101
        else -> 900
    }
}
