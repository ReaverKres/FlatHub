package listing.kz.olx

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * OLX.kz region_id + city_id. See tmp/kz/api/olx/NOTES.md.
 * HTML city slug for Almaty is `alma-ata` (not used by REST API).
 */
data class OlxKzCityIds(val regionId: Int, val cityId: Int)

object OlxKzCities {
    fun idsFor(city: CityCode?): OlxKzCityIds = when (city) {
        null, CityCode.ALMATY -> OlxKzCityIds(regionId = 8, cityId = 1)
        CityCode.ASTANA -> OlxKzCityIds(regionId = 13, cityId = 87)
        CityCode.SHYMKENT -> OlxKzCityIds(regionId = 6, cityId = 47)
        CityCode.KARAGANDA -> OlxKzCityIds(regionId = 5, cityId = 39)
        else -> OlxKzCityIds(regionId = 8, cityId = 1)
    }

    /** Flat rent 2979 / flat sale 2982. */
    fun categoryId(isSale: Boolean): Int = if (isSale) 2982 else 2979
}
