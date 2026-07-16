package listing.pl.morizon

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Morizon listing URL paths.
 * Warsaw rent confirmed: `/do-wynajecia/mieszkania/warszawa/`
 */
object MorizonCities {
    fun citySlug(city: CityCode?): String = when (city) {
        null, CityCode.WARSZAWA -> "warszawa"
        CityCode.KRAKOW -> "krakow"
        CityCode.WROCLAW -> "wroclaw"
        CityCode.POZNAN -> "poznan"
        CityCode.GDANSK -> "gdansk"
        CityCode.LODZ -> "lodz"
        CityCode.SZCZECIN -> "szczecin"
        CityCode.LUBLIN -> "lublin"
        CityCode.BYDGOSZCZ -> "bydgoszcz"
        CityCode.KATOWICE -> "katowice"
        else -> "warszawa"
    }

    fun listingUrl(
        city: CityCode?,
        adType: AdType,
        isCommercial: Boolean,
        page: Int,
    ): String {
        val estate = if (isCommercial) "lokale-uzytkowe" else "mieszkania"
        val transaction = when (adType) {
            is AdType.SALE -> "na-sprzedaz"
            else -> "do-wynajecia"
        }
        val base = "/$transaction/$estate/${citySlug(city)}/"
        return if (page > 1) "$base?page=$page" else base
    }
}
