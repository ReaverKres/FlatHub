package listing.pl.otodom

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Otodom location path segments: voivodeship/county/commune/city
 * See tmp/pl/api/otodom/NOTES.md
 */
object OtodomCities {
    fun pathFor(city: CityCode?): String = when (city) {
        null, CityCode.WARSZAWA -> "mazowieckie/warszawa/warszawa/warszawa"
        CityCode.KRAKOW -> "malopolskie/krakow/krakow/krakow"
        CityCode.WROCLAW -> "dolnoslaskie/wroclaw/wroclaw/wroclaw"
        CityCode.POZNAN -> "wielkopolskie/poznan/poznan/poznan"
        CityCode.GDANSK -> "pomorskie/gdansk/gdansk/gdansk"
        CityCode.LODZ -> "lodzkie/lodz/lodz/lodz"
        CityCode.SZCZECIN -> "zachodniopomorskie/szczecin/szczecin/szczecin"
        CityCode.LUBLIN -> "lubelskie/lublin/lublin/lublin"
        CityCode.BYDGOSZCZ -> "kujawsko-pomorskie/bydgoszcz/bydgoszcz/bydgoszcz"
        CityCode.KATOWICE -> "slaskie/katowice/katowice/katowice"
        else -> "mazowieckie/warszawa/warszawa/warszawa"
    }
}
