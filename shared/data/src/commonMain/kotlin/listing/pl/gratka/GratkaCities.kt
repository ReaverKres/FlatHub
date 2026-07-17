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
        priceFrom: Int? = null,
        priceTo: Int? = null,
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
        val query = buildList {
            if (page > 1) add("page=$page")
            if (priceFrom != null) add("cena-calkowita:min=$priceFrom")
            if (priceTo != null) add("cena-calkowita:max=$priceTo")
        }
        return if (query.isEmpty()) base else "$base?${query.joinToString("&")}"
    }
}
