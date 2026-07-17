package listing.tr.emlakjet

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Emlakjet city path segments: `/kiralik-konut/{slug}/` or `/satilik-konut/{slug}/`.
 * See tmp/tr/api/emlakjet/NOTES.md.
 */
object EmlakjetCities {
    fun slug(city: CityCode?): String = when (city) {
        null, CityCode.ISTANBUL -> "istanbul"
        CityCode.ANKARA -> "ankara"
        CityCode.IZMIR -> "izmir"
        CityCode.ANTALYA -> "antalya"
        CityCode.BURSA -> "bursa"
        CityCode.ADANA -> "adana"
        CityCode.GAZIANTEP -> "gaziantep"
        CityCode.KONYA -> "konya"
        else -> "istanbul"
    }
}
