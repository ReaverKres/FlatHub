package listing.de.immowelt

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Immowelt.de search path segments. See tmp/de/api/immowelt/city_geo_map.tsv.
 */
object ImmoweltCities {
    data class CityPath(
        val region: String,
        val cityPlz: String,
        val geoId: String,
    )

    fun path(city: CityCode?): CityPath = when (city) {
        null, CityCode.BERLIN -> CityPath("berlin", "berlin-10115", "ad08de8634")
        CityCode.MUENCHEN -> CityPath("bayern", "munchen-80331", "ad08de6345")
        CityCode.HAMBURG -> CityPath("hamburg", "hamburg-20095", "ad08de1113")
        CityCode.KOELN -> CityPath("nordrhein-westfalen", "koln-50769", "ad08de2179")
        CityCode.FRANKFURT -> CityPath("hessen", "frankfurt-am-main-60308", "ad08de2509")
        CityCode.STUTTGART -> CityPath("baden-wurttemberg", "stuttgart-70173", "ad08de5241")
        CityCode.DUESSELDORF -> CityPath("nordrhein-westfalen", "dusseldorf-40210", "ad08de2112")
        CityCode.LEIPZIG -> CityPath("sachsen", "leipzig-04103", "ad08de10168")
        else -> path(CityCode.BERLIN)
    }
}
