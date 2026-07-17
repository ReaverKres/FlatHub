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
    val almatyUiItem = UiCityItem(CityCode.ALMATY, "Алматы", Coordinates(43.2389, 76.9455))
    val madridUiItem = UiCityItem(CityCode.MADRID, "Madrid", Coordinates(40.4168, -3.7038))
    val berlinUiItem = UiCityItem(CityCode.BERLIN, "Berlin", Coordinates(52.5200, 13.4050))
    val istanbulUiItem = UiCityItem(CityCode.ISTANBUL, "İstanbul", Coordinates(41.0082, 28.9784))
    val dubaiUiItem = UiCityItem(CityCode.DUBAI, "Dubai", Coordinates(25.2048, 55.2708))
    val bangkokUiItem = UiCityItem(CityCode.BANGKOK, "Bangkok", Coordinates(13.7563, 100.5018))

    fun countries(): List<UiCountryItem> = listOf(
        UiCountryItem(CountryCode.BY, "Беларусь"),
        UiCountryItem(CountryCode.PL, "Polska"),
        UiCountryItem(CountryCode.GE, "Georgia"),
        UiCountryItem(CountryCode.KZ, "Қазақстан"),
        UiCountryItem(CountryCode.ES, "España"),
        UiCountryItem(CountryCode.DE, "Deutschland"),
        UiCountryItem(CountryCode.TR, "Türkiye"),
        UiCountryItem(CountryCode.AE, "UAE"),
        UiCountryItem(CountryCode.TH, "Thailand"),
    )

    fun defaultCity(country: CountryCode): UiCityItem = when (country) {
        CountryCode.PL -> warszawaUiItem
        CountryCode.GE -> tbilisiUiItem
        CountryCode.KZ -> almatyUiItem
        CountryCode.ES -> madridUiItem
        CountryCode.DE -> berlinUiItem
        CountryCode.TR -> istanbulUiItem
        CountryCode.AE -> dubaiUiItem
        CountryCode.TH -> bangkokUiItem
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

        CountryCode.KZ -> listOf(
            almatyUiItem,
            UiCityItem(CityCode.ASTANA, "Астана", Coordinates(51.1694, 71.4491)),
            UiCityItem(CityCode.SHYMKENT, "Шымкент", Coordinates(42.3417, 69.5901)),
            UiCityItem(CityCode.KARAGANDA, "Қарағанды", Coordinates(49.8047, 73.1094)),
        )

        CountryCode.ES -> listOf(
            madridUiItem,
            UiCityItem(CityCode.BARCELONA, "Barcelona", Coordinates(41.3874, 2.1686)),
            UiCityItem(CityCode.VALENCIA, "Valencia", Coordinates(39.4699, -0.3763)),
            UiCityItem(CityCode.SEVILLA, "Sevilla", Coordinates(37.3891, -5.9845)),
            UiCityItem(CityCode.MALAGA, "Málaga", Coordinates(36.7213, -4.4214)),
            UiCityItem(CityCode.ZARAGOZA, "Zaragoza", Coordinates(41.6488, -0.8891)),
        )

        CountryCode.DE -> listOf(
            berlinUiItem,
            UiCityItem(CityCode.MUENCHEN, "München", Coordinates(48.1351, 11.5820)),
            UiCityItem(CityCode.HAMBURG, "Hamburg", Coordinates(53.5511, 9.9937)),
            UiCityItem(CityCode.KOELN, "Köln", Coordinates(50.9375, 6.9603)),
            UiCityItem(CityCode.FRANKFURT, "Frankfurt am Main", Coordinates(50.1109, 8.6821)),
            UiCityItem(CityCode.STUTTGART, "Stuttgart", Coordinates(48.7758, 9.1829)),
            UiCityItem(CityCode.DUESSELDORF, "Düsseldorf", Coordinates(51.2277, 6.7735)),
            UiCityItem(CityCode.LEIPZIG, "Leipzig", Coordinates(51.3397, 12.3731)),
        )

        CountryCode.TR -> listOf(
            istanbulUiItem,
            UiCityItem(CityCode.ANKARA, "Ankara", Coordinates(39.9334, 32.8597)),
            UiCityItem(CityCode.IZMIR, "İzmir", Coordinates(38.4237, 27.1428)),
            UiCityItem(CityCode.ANTALYA, "Antalya", Coordinates(36.8969, 30.7133)),
            UiCityItem(CityCode.BURSA, "Bursa", Coordinates(40.1885, 29.0610)),
            UiCityItem(CityCode.ADANA, "Adana", Coordinates(37.0000, 35.3213)),
            UiCityItem(CityCode.GAZIANTEP, "Gaziantep", Coordinates(37.0662, 37.3833)),
            UiCityItem(CityCode.KONYA, "Konya", Coordinates(37.8746, 32.4932)),
        )

        CountryCode.AE -> listOf(
            dubaiUiItem,
            UiCityItem(CityCode.ABU_DHABI, "Abu Dhabi", Coordinates(24.4539, 54.3773)),
            UiCityItem(CityCode.SHARJAH, "Sharjah", Coordinates(25.3463, 55.4209)),
            UiCityItem(CityCode.AJMAN, "Ajman", Coordinates(25.4052, 55.5136)),
            UiCityItem(CityCode.AL_AIN, "Al Ain", Coordinates(24.2075, 55.7447)),
            UiCityItem(CityCode.RAS_AL_KHAIMAH, "Ras Al Khaimah", Coordinates(25.7895, 55.9432)),
            UiCityItem(CityCode.FUJAIRAH, "Fujairah", Coordinates(25.1288, 56.3265)),
            UiCityItem(CityCode.UMM_AL_QUWAIN, "Umm Al Quwain", Coordinates(25.5647, 55.5552)),
        )

        CountryCode.TH -> listOf(
            bangkokUiItem,
            UiCityItem(CityCode.PHUKET, "Phuket", Coordinates(7.8804, 98.3923)),
            UiCityItem(CityCode.CHIANG_MAI, "Chiang Mai", Coordinates(18.7883, 98.9853)),
            UiCityItem(CityCode.PATTAYA, "Pattaya", Coordinates(12.9236, 100.8825)),
            UiCityItem(CityCode.HUA_HIN, "Hua Hin", Coordinates(12.5684, 99.9577)),
            UiCityItem(CityCode.KOH_SAMUI, "Koh Samui", Coordinates(9.5120, 100.0136)),
        )
    }

    /** Backward-compatible default (BY). */
    fun cities(): List<UiCityItem> = cities(CountryCode.BY)

    fun findSelectedCity(cityCode: CityCode): UiCityItem =
        (
                cities(CountryCode.BY) +
                        cities(CountryCode.PL) +
                        cities(CountryCode.GE) +
                        cities(CountryCode.KZ) +
                        cities(CountryCode.ES) +
                        cities(CountryCode.DE) +
                        cities(CountryCode.TR) +
                        cities(CountryCode.AE) +
                        cities(CountryCode.TH)
                ).find { it.code == cityCode }
            ?: minskUiItem

    fun countryForCity(cityCode: CityCode): CountryCode =
        when (cityCode) {
            in cities(CountryCode.PL).map { it.code } -> CountryCode.PL
            in cities(CountryCode.GE).map { it.code } -> CountryCode.GE
            in cities(CountryCode.KZ).map { it.code } -> CountryCode.KZ
            in cities(CountryCode.ES).map { it.code } -> CountryCode.ES
            in cities(CountryCode.DE).map { it.code } -> CountryCode.DE
            in cities(CountryCode.TR).map { it.code } -> CountryCode.TR
            in cities(CountryCode.AE).map { it.code } -> CountryCode.AE
            in cities(CountryCode.TH).map { it.code } -> CountryCode.TH
            else -> CountryCode.BY
        }

    fun countryDisplayName(code: CountryCode): String =
        countries().find { it.code == code }?.displayName ?: code.name
}
