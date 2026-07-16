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
                line = when (info.line) {
                    MetroLine.BLUE -> MetroLineState.BLUE
                    MetroLine.RED -> MetroLineState.RED
                    MetroLine.GREEN -> MetroLineState.GREEN
                }
            )
        }
    }
}


