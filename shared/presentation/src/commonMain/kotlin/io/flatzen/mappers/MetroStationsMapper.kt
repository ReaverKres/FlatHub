package io.flatzen.mappers

import entities.MetroStations
import entities.MetroLine
import io.flatzen.viewmodel.filter.MetroLineState
import io.flatzen.viewmodel.filter.UiMetroStation

object MetroStationsMapper {

    fun allStationsOrderedForUi(): List<UiMetroStation> {
        return MetroStations.allStationsRequest().map { info ->
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


