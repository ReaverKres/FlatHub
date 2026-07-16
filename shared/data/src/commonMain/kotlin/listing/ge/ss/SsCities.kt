package listing.ge.ss

import io.flatzen.commoncomponents.commonentities.CityCode

object SsCities {
    /** home.ss.ge pageProps.locations — Tbilisi = 95 */
    fun cityId(city: CityCode?): Int = when (city) {
        CityCode.BATUMI -> 96
        CityCode.KUTAISI -> 97
        CityCode.RUSTAVI -> 98
        else -> 95 // TBILISI default
    }
}
