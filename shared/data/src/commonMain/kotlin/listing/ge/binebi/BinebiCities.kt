package listing.ge.binebi

import io.flatzen.commoncomponents.commonentities.CityCode

object BinebiCities {
    /** Confirmed via GET title probes: 1 თბილისი, 4 ბათუმი, 2 ქუთაისი, 3 რუსთავი */
    fun cityId(city: CityCode?): Int = when (city) {
        CityCode.BATUMI -> 4
        CityCode.KUTAISI -> 2
        CityCode.RUSTAVI -> 3
        else -> 1 // TBILISI
    }
}
