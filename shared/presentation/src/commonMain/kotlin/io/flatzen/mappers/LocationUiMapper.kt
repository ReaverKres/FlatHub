package io.flatzen.mappers

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.CountryCode

object LocationUiMapper {
    data class UiCityItem(val code: CityCode, val displayName: String, val coordinates: Coordinates)
    data class UiCountryItem(val code: CountryCode, val displayName: String)

    val minskUiItem = UiCityItem(CityCode.MINSK, "Минск", Coordinates(53.902147, 27.561388))
    val warszawaUiItem = UiCityItem(CityCode.WARSZAWA, "Warszawa", Coordinates(52.2297, 21.0122))
    val tbilisiUiItem = UiCityItem(CityCode.TBILISI, "Tbilisi", Coordinates(41.7151, 44.8271))

    fun countries(): List<UiCountryItem> = listOf(
        UiCountryItem(CountryCode.BY, "Беларусь"),
        UiCountryItem(CountryCode.PL, "Polska"),
        UiCountryItem(CountryCode.GE, "Georgia"),
    )

    fun defaultCity(country: CountryCode): UiCityItem = when (country) {
        CountryCode.PL -> warszawaUiItem
        CountryCode.GE -> tbilisiUiItem
        CountryCode.BY -> minskUiItem
    }

    fun cities(country: CountryCode = CountryCode.BY): List<UiCityItem> = when (country) {
        CountryCode.BY -> listOf(
            minskUiItem,
            UiCityItem(CityCode.BREST, "Брест", Coordinates(52.093825, 23.684889)),
            UiCityItem(CityCode.VITEBSK, "Витебск", Coordinates(55.184217, 30.202878)),
            UiCityItem(CityCode.GOMEL, "Гомель", Coordinates(52.424160, 31.014281)),
            UiCityItem(CityCode.GRODNO, "Гродно", Coordinates(53.677839, 23.829529)),
            UiCityItem(CityCode.MOGILEV, "Могилёв", Coordinates(53.894548, 30.330654)),
        )

        CountryCode.PL -> listOf(
            warszawaUiItem,
            UiCityItem(CityCode.KRAKOW, "Kraków", Coordinates(50.0647, 19.9450)),
            UiCityItem(CityCode.WROCLAW, "Wrocław", Coordinates(51.1079, 17.0385)),
            UiCityItem(CityCode.POZNAN, "Poznań", Coordinates(52.4064, 16.9252)),
            UiCityItem(CityCode.GDANSK, "Gdańsk", Coordinates(54.3520, 18.6466)),
            UiCityItem(CityCode.LODZ, "Łódź", Coordinates(51.7592, 19.4560)),
            UiCityItem(CityCode.SZCZECIN, "Szczecin", Coordinates(53.4285, 14.5528)),
            UiCityItem(CityCode.LUBLIN, "Lublin", Coordinates(51.2465, 22.5684)),
            UiCityItem(CityCode.BYDGOSZCZ, "Bydgoszcz", Coordinates(53.1235, 18.0084)),
            UiCityItem(CityCode.KATOWICE, "Katowice", Coordinates(50.2649, 19.0238)),
        )

        CountryCode.GE -> listOf(
            tbilisiUiItem,
            UiCityItem(CityCode.BATUMI, "Batumi", Coordinates(41.6168, 41.6367)),
            UiCityItem(CityCode.KUTAISI, "Kutaisi", Coordinates(42.2679, 42.6946)),
            UiCityItem(CityCode.RUSTAVI, "Rustavi", Coordinates(41.5495, 44.9932)),
        )
    }

    /** Backward-compatible default (BY). */
    fun cities(): List<UiCityItem> = cities(CountryCode.BY)

    fun findSelectedCity(cityCode: CityCode): UiCityItem =
        (
                cities(CountryCode.BY) +
                        cities(CountryCode.PL) +
                        cities(CountryCode.GE)
                ).find { it.code == cityCode }
            ?: minskUiItem

    fun countryForCity(cityCode: CityCode): CountryCode =
        when (cityCode) {
            in cities(CountryCode.PL).map { it.code } -> CountryCode.PL
            in cities(CountryCode.GE).map { it.code } -> CountryCode.GE
            else -> CountryCode.BY
        }

    fun countryDisplayName(code: CountryCode): String =
        countries().find { it.code == code }?.displayName ?: code.name
}
