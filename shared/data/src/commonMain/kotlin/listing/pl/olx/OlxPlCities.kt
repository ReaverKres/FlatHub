package listing.pl.olx

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * OLX.pl region_id + city_id for major cities.
 * Confirmed from city search HTML (city_id / region_id in page payload), 2026-07.
 */
data class OlxCityIds(val regionId: Int, val cityId: Int)

object OlxPlCities {
    fun idsFor(city: CityCode?): OlxCityIds = when (city) {
        null, CityCode.WARSZAWA -> OlxCityIds(regionId = 2, cityId = 17871)
        CityCode.KRAKOW -> OlxCityIds(regionId = 4, cityId = 8959)
        CityCode.WROCLAW -> OlxCityIds(regionId = 3, cityId = 19701)
        CityCode.POZNAN -> OlxCityIds(regionId = 1, cityId = 13983)
        CityCode.GDANSK -> OlxCityIds(regionId = 5, cityId = 5659)
        CityCode.LODZ -> OlxCityIds(regionId = 7, cityId = 10609)
        CityCode.SZCZECIN -> OlxCityIds(regionId = 11, cityId = 16705)
        CityCode.LUBLIN -> OlxCityIds(regionId = 8, cityId = 10119)
        CityCode.BYDGOSZCZ -> OlxCityIds(regionId = 15, cityId = 4019)
        CityCode.KATOWICE -> OlxCityIds(regionId = 6, cityId = 7691)
        else -> OlxCityIds(regionId = 2, cityId = 17871)
    }

    fun categoryId(isSale: Boolean, isRoom: Boolean, isCommercial: Boolean): Int = when {
        isCommercial -> if (isSale) 125 else 127
        isRoom -> 11
        isSale -> 14
        else -> 15
    }
}
