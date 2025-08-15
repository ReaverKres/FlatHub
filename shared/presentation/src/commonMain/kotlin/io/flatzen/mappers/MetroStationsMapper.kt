package io.flatzen.mappers

import entities.MetroStations
import entities.MetroLine
import io.flatzen.states.MetroLineState
import io.flatzen.states.UiMetroStation

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


