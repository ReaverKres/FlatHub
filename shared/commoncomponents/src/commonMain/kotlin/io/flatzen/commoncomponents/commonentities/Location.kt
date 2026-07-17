package io.flatzen.commoncomponents.commonentities

data class LocationCommon(
    val country: Country,
    val selectedCity: City
)

data class City(val cityCode: CityCode, val coordinates: Coordinates)

data class Country(val country: CountryCode, val allCities: List<City>)

enum class CountryCode {
    BY, PL, GE, KZ, ES;

    companion object {
        fun fromNetworkIso(iso: String?): CountryCode = when (iso?.uppercase()) {
            "BY" -> BY
            "PL" -> PL
            "GE" -> GE
            "KZ" -> KZ
            "ES" -> ES
            else -> BY
        }
    }
}

enum class CityCode {
    // Belarus
    MINSK, BREST, VITEBSK, GOMEL, GRODNO, MOGILEV,

    // Poland (major cities)
    WARSZAWA, KRAKOW, WROCLAW, POZNAN, GDANSK, LODZ,
    SZCZECIN, LUBLIN, BYDGOSZCZ, KATOWICE,

    // Georgia (MVP)
    TBILISI, BATUMI, KUTAISI, RUSTAVI,

    // Kazakhstan (MVP)
    ALMATY, ASTANA, SHYMKENT, KARAGANDA,

    // Spain (MVP)
    MADRID, BARCELONA, VALENCIA, SEVILLA, MALAGA, ZARAGOZA,
}

/** Only BY sources currently map commercial property subtypes. */
fun CountryCode.supportsCommercialPropertyTypeFilter(): Boolean = this == CountryCode.BY

fun CountryCode.defaultCityCode(): CityCode = when (this) {
    CountryCode.BY -> CityCode.MINSK
    CountryCode.PL -> CityCode.WARSZAWA
    CountryCode.GE -> CityCode.TBILISI
    CountryCode.KZ -> CityCode.ALMATY
    CountryCode.ES -> CityCode.MADRID
}

object Location {
    fun mapCityCodeToDomainName(cityCode: CityCode) = when (cityCode) {
        CityCode.MINSK -> "minsk"
        CityCode.BREST -> "brest"
        CityCode.VITEBSK -> "vitebsk"
        CityCode.GOMEL -> "gomel"
        CityCode.GRODNO -> "grodno"
        CityCode.MOGILEV -> "mogilev"
        CityCode.WARSZAWA -> "warszawa"
        CityCode.KRAKOW -> "krakow"
        CityCode.WROCLAW -> "wroclaw"
        CityCode.POZNAN -> "poznan"
        CityCode.GDANSK -> "gdansk"
        CityCode.LODZ -> "lodz"
        CityCode.SZCZECIN -> "szczecin"
        CityCode.LUBLIN -> "lublin"
        CityCode.BYDGOSZCZ -> "bydgoszcz"
        CityCode.KATOWICE -> "katowice"
        CityCode.TBILISI -> "tbilisi"
        CityCode.BATUMI -> "batumi"
        CityCode.KUTAISI -> "kutaisi"
        CityCode.RUSTAVI -> "rustavi"
        CityCode.ALMATY -> "almaty"
        CityCode.ASTANA -> "astana"
        CityCode.SHYMKENT -> "shymkent"
        CityCode.KARAGANDA -> "karaganda"
        CityCode.MADRID -> "madrid"
        CityCode.BARCELONA -> "barcelona"
        CityCode.VALENCIA -> "valencia"
        CityCode.SEVILLA -> "sevilla"
        CityCode.MALAGA -> "malaga"
        CityCode.ZARAGOZA -> "zaragoza"
    }
}
