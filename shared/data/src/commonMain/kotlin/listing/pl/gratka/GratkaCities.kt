package listing.pl.gratka

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Gratka listing URL path segments (city slug).
 * Warsaw rent confirmed: `/nieruchomosci/mieszkania/warszawa/wynajem`
 */
object GratkaCities {
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
        isRoom: Boolean,
        isCommercial: Boolean,
        page: Int,
    ): String {
        val estate = when {
            isCommercial -> "lokale-uzytkowe"
            isRoom -> "pokoje"
            else -> "mieszkania"
        }
        val transaction = when (adType) {
            is AdType.SALE -> "sprzedaz"
            else -> "wynajem"
        }
        val base = "/nieruchomosci/$estate/${citySlug(city)}/$transaction"
        return if (page > 1) "$base?page=$page" else base
    }
}
