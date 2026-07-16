package io.flatzen.commoncomponents.commonentities

data class LocationCommon(
    val country: Country,
    val selectedCity: City
)

data class City(val cityCode: CityCode, val coordinates: Coordinates)

data class Country(val country: CountryCode, val allCities: List<City>)

enum class CountryCode { BY, PL }

enum class CityCode {
    // Belarus
    MINSK, BREST, VITEBSK, GOMEL, GRODNO, MOGILEV,

    // Poland (major cities)
    WARSZAWA, KRAKOW, WROCLAW, POZNAN, GDANSK, LODZ,
    SZCZECIN, LUBLIN, BYDGOSZCZ, KATOWICE,
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
    }
}
