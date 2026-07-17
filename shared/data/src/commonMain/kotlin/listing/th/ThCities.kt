package listing.th

import io.flatzen.commoncomponents.commonentities.CityCode

/** Zone slugs shared by PropertyHub / RentHub `_next/data` paths. */
object ThCities {
    fun zoneSlug(city: CityCode?): String = when (city) {
        null, CityCode.BANGKOK -> "bangkok"
        CityCode.PHUKET -> "phuket"
        CityCode.CHIANG_MAI -> "chiang-mai"
        CityCode.PATTAYA -> "pattaya"
        CityCode.HUA_HIN -> "hua-hin"
        CityCode.KOH_SAMUI -> "koh-samui"
        else -> "bangkok"
    }
}
