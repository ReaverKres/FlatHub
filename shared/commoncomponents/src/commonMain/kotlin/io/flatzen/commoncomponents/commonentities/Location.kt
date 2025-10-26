package io.flatzen.commoncomponents.commonentities

data class LocationCommon(
    val country: Country,
    val selectedCity: City
)

data class City(val cityCode: CityCode, val coordinates: Coordinates)

data class Country(val country: CountryCode, val allCities: List<City>)

enum class CountryCode { BY }

enum class CityCode {
    MINSK, BREST, VITEBSK, GOMEL, GRODNO, MOGILEV
}

object Location {
    fun mapCityCodeToDomainName(cityCode: CityCode) = when(cityCode) {
        CityCode.MINSK -> "minsk"
        CityCode.BREST -> "brest"
        CityCode.VITEBSK -> "vitebsk"
        CityCode.GOMEL -> "gomel"
        CityCode.GRODNO -> "grodno"
        CityCode.MOGILEV -> "mogilev"
    }
}