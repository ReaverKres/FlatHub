package io.flatzen.mappers

import entities.KufarMetroStations
import entities.MetroLine
import io.flatzen.states.MetroLineState

object MetroStationsMapper {

    data class UiMetroStation(
        val id: Int,
        val name: String,
        val line: MetroLineState
    )

    fun allStationsOrderedForUi(): List<UiMetroStation> {
        return KufarMetroStations.allStationsOrderedForUi().map { info ->
            UiMetroStation(
                id = info.id,
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


