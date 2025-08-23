package io.flatzen.viewmodel

import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

class MapViewModel(
    private val tileStreamProvider: TileStreamProvider
) : BaseMviViewModel<MapAction, MapUiState, MapEvent, MapEffect>() {

    private val maxLevel = 16
    private val minLevel = 12
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    private val minsk = lonLatToNormalized(53.902147, 27.561388) // Minsk
    private val x = minsk.first
    private val y = minsk.second
    val mapState = MapState(levelCount = maxLevel + 1, mapSize, mapSize) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
        scroll(x, y)
        scale(0.0) // to zoom out initially
    }.apply {
        addLayer(tileStreamProvider)
        enableRotation()
    }

    override fun initialState(): MapUiState = MapUiState.Loading

    override suspend fun handleIntent(action: MapAction, currentState: MapUiState): Flow<MapEvent> = flow {
        when (action) {
            MapAction.Initialize -> {
                emit(MapEvent.Ready)
            }
            MapAction.CenterOnWorld -> {
                // Центрируемся на произвольной точке мира (0.5, 0.5)
                mapState.scrollTo(0.5, 0.5, destScale = 1.0)
            }
            MapAction.ZoomIn -> { /* no-op for now */ }
            MapAction.ZoomOut -> { /* no-op for now */ }
        }
    }

    override suspend fun reduce(event: MapEvent, currentState: MapUiState): MapUiState = when (event) {
        MapEvent.Ready -> MapUiState.Ready
    }
}

sealed interface MapUiState : MviState {
    data object Loading : MapUiState
    data object Ready : MapUiState
}

sealed interface MapAction : MviAction {
    data object Initialize : MapAction
    data object CenterOnWorld : MapAction
    data object ZoomIn : MapAction
    data object ZoomOut : MapAction
}

sealed interface MapEvent : MviEvent {
    data object Ready : MapEvent
}

sealed interface MapEffect : MviEffect


