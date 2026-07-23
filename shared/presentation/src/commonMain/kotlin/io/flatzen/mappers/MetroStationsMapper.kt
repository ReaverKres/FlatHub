package io.flatzen.mappers

import entities.MetroLine
import entities.MetroStations
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.viewmodel.filter.MetroLineState
import io.flatzen.viewmodel.filter.UiMetroStation

object MetroStationsMapper {

    fun allStationsOrderedForUi(): List<UiMetroStation> =
        stationsForCity(CityCode.MINSK)

    fun stationsForCity(city: CityCode?): List<UiMetroStation> {
        return MetroStations.stationsForCity(city).map { info ->
            UiMetroStation(
                name = info.name,
                line = info.line.toUiState(),
            )
        }
    }

    fun hasStations(city: CityCode?): Boolean =
        MetroStations.stationsForCity(city).isNotEmpty()

    /** Display order of line columns for the metro filter screen. */
    fun lineOrderForCity(city: CityCode?): List<MetroLineState> = when (city) {
        CityCode.LONDON -> listOf(
            MetroLineState.BAKERLOO, MetroLineState.CENTRAL, MetroLineState.CIRCLE,
            MetroLineState.DISTRICT, MetroLineState.HAMMERSMITH_CITY, MetroLineState.JUBILEE,
            MetroLineState.METROPOLITAN, MetroLineState.NORTHERN, MetroLineState.PICCADILLY,
            MetroLineState.VICTORIA, MetroLineState.WATERLOO_CITY, MetroLineState.ELIZABETH,
        )

        CityCode.PARIS -> listOf(
            MetroLineState.PARIS_M1, MetroLineState.PARIS_M2, MetroLineState.PARIS_M3,
            MetroLineState.PARIS_M4, MetroLineState.PARIS_M5, MetroLineState.PARIS_M6,
            MetroLineState.PARIS_M7, MetroLineState.PARIS_M8, MetroLineState.PARIS_M9,
            MetroLineState.PARIS_M10, MetroLineState.PARIS_M11, MetroLineState.PARIS_M12,
            MetroLineState.PARIS_M13, MetroLineState.PARIS_M14,
            MetroLineState.PARIS_RER_A, MetroLineState.PARIS_RER_B,
        )

        CityCode.WIEN -> listOf(
            MetroLineState.WIEN_U1, MetroLineState.WIEN_U2, MetroLineState.WIEN_U3,
            MetroLineState.WIEN_U4, MetroLineState.WIEN_U6,
        )

        CityCode.SEOUL -> listOf(
            MetroLineState.SEOUL_1, MetroLineState.SEOUL_2, MetroLineState.SEOUL_3,
            MetroLineState.SEOUL_4, MetroLineState.SEOUL_5, MetroLineState.SEOUL_6,
            MetroLineState.SEOUL_7, MetroLineState.SEOUL_8, MetroLineState.SEOUL_9,
        )

        CityCode.BERLIN -> listOf(
            MetroLineState.BERLIN_U1, MetroLineState.BERLIN_U2, MetroLineState.BERLIN_U3,
            MetroLineState.BERLIN_U4, MetroLineState.BERLIN_U5, MetroLineState.BERLIN_U6,
            MetroLineState.BERLIN_U7, MetroLineState.BERLIN_U8, MetroLineState.BERLIN_U9,
            MetroLineState.BERLIN_S1, MetroLineState.BERLIN_S2, MetroLineState.BERLIN_S3,
            MetroLineState.BERLIN_S5, MetroLineState.BERLIN_S7, MetroLineState.BERLIN_S9,
        )

        CityCode.MADRID -> listOf(
            MetroLineState.MADRID_1, MetroLineState.MADRID_2, MetroLineState.MADRID_3,
            MetroLineState.MADRID_4, MetroLineState.MADRID_5, MetroLineState.MADRID_6,
            MetroLineState.MADRID_7, MetroLineState.MADRID_8, MetroLineState.MADRID_9,
            MetroLineState.MADRID_10, MetroLineState.MADRID_11, MetroLineState.MADRID_12,
            MetroLineState.MADRID_R,
        )

        CityCode.BARCELONA -> listOf(
            MetroLineState.BCN_L1, MetroLineState.BCN_L2, MetroLineState.BCN_L3,
            MetroLineState.BCN_L4, MetroLineState.BCN_L5, MetroLineState.BCN_L9N,
            MetroLineState.BCN_L9S, MetroLineState.BCN_L10, MetroLineState.BCN_L11,
        )

        CityCode.BANGKOK -> listOf(
            MetroLineState.BKK_BTS_SUKHUMVIT, MetroLineState.BKK_BTS_SILOM,
            MetroLineState.BKK_MRT_BLUE, MetroLineState.BKK_MRT_PURPLE, MetroLineState.BKK_ARL,
        )

        CityCode.TOKYO -> listOf(
            MetroLineState.TOKYO_YAMANOTE,
            MetroLineState.TOKYO_GINZA,
            MetroLineState.TOKYO_MARUNOUCHI,
            MetroLineState.TOKYO_HIBIYA,
            MetroLineState.TOKYO_TOZAI,
            MetroLineState.TOKYO_CHIYODA,
            MetroLineState.TOKYO_YURAKUCHO,
            MetroLineState.TOKYO_HANZOMON,
            MetroLineState.TOKYO_NAMBOKU,
            MetroLineState.TOKYO_FUKUTOSHIN,
            MetroLineState.TOKYO_ASAKUSA,
            MetroLineState.TOKYO_MITA,
            MetroLineState.TOKYO_SHINJUKU,
            MetroLineState.TOKYO_OEDO,
        )

        CityCode.WARSZAWA, CityCode.TBILISI -> listOf(
            MetroLineState.BLUE, MetroLineState.RED,
        )

        else -> listOf(MetroLineState.BLUE, MetroLineState.RED, MetroLineState.GREEN)
    }

    fun MetroLine.toUiState(): MetroLineState = MetroLineState.valueOf(name)
}
