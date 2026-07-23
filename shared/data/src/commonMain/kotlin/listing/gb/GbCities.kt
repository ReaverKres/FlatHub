package listing.gb

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.Location

/** Shared URL slugs for GB listing sites (OTM path segment, OpenRent city segment, etc.). */
object GbCities {
    fun onTheMarketSlug(city: CityCode?): String =
        Location.mapCityCodeToDomainName(city ?: CityCode.LONDON)
}
