package io.flatzen.viewmodel

import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class MapViewModel(
    private val tileStreamProvider: TileStreamProvider
) : BaseMviViewModel<MapAction, MapUiState, MapEvent, MapEffect>() {

    val mapState: MapState = MapState(
        4, 4096, 4096
    ).apply {
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


