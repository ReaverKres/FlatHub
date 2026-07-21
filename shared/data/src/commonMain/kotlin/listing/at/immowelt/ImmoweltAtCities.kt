package listing.at.immowelt

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Immowelt.at search path + geoId. See tmp/at/api/immowelt/city_geo_map.tsv.
 * Extra MVP cities without verified geoId fall back to Wien.
 */
object ImmoweltAtCities {
    data class CityPath(
        val path: String,
        val geoId: String,
    )

    fun path(city: CityCode?): CityPath = when (city) {
        null, CityCode.WIEN -> CityPath("wien", "ad04at9")
        CityCode.GRAZ -> CityPath("steiermark/graz-601", "ad06at68")
        CityCode.SALZBURG -> CityPath("salzburg", "ad04at5")
        CityCode.LINZ -> CityPath("oberosterreich/linz-4020", "ad08at877")
        CityCode.INNSBRUCK -> CityPath("tirol/innsbruck-6020", "ad08at1720")
        else -> path(CityCode.WIEN)
    }
}
