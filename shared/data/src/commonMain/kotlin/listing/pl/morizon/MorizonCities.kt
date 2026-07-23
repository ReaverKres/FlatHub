package listing.pl.morizon

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import listing.pl.isPlSaleDeal

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
        priceFrom: Int? = null,
        priceTo: Int? = null,
    ): String {
        val estate = if (isCommercial) "lokale-uzytkowe" else "mieszkania"
        // Rent: /do-wynajecia/mieszkania/warszawa/
        // Sale: /sprzedaz/mieszkania/warszawa/  (NOT /na-sprzedaz — that returns searchResult: null)
        val transaction = if (adType.isPlSaleDeal()) "sprzedaz" else "do-wynajecia"
        val base = "/$transaction/$estate/${citySlug(city)}/"
        val query = buildList {
            if (page > 1) add("page=$page")
            if (priceFrom != null) add("ps[price_from]=$priceFrom")
            if (priceTo != null) add("ps[price_to]=$priceTo")
        }
        return if (query.isEmpty()) base else "$base?${query.joinToString("&")}"
    }
}
