package listing.ge.livo

import io.flatzen.commoncomponents.commonentities.CityCode

object LivoCities {
    /** api.livo.ge statement-parameters cities */
    fun cityId(city: CityCode?): Int = when (city) {
        CityCode.BATUMI -> 15
        CityCode.KUTAISI -> 96
        CityCode.RUSTAVI -> 73
        else -> 1 // TBILISI
    }
}
