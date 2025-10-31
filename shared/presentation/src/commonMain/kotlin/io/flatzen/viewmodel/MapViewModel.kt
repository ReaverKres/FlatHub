package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.viewmodel.MapEvent.DialogStateUpdated
import io.flatzen.viewmodel.MapEvent.FirstPointInPathEvent
import io.flatzen.viewmodel.MapEvent.HideMapArea
import io.flatzen.viewmodel.MapEvent.Ready
import io.flatzen.viewmodel.MapEvent.ShowMapArea
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.SaveDialogState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.hasPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow

class MapViewModel(
    private val tileStreamProvider: TileStreamProvider
) : BaseMviViewModel<MapAction, MapUiState, MapEvent, MapEffect>() {

    private val maxLevel = 18
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

    private val pathId: String = "pathId"
    private val pathDataPoints: MutableMap<String, MutableList<Pair<Double, Double>>> =
        mutableMapOf()

    override fun initialState(): MapUiState = MapUiState()

    override suspend fun handleIntent(action: MapAction, currentState: MapUiState): Flow<MapEvent> {
        return when (action) {
            MapAction.Initialize -> {
                flowOf(Ready)
            }

            MapAction.CenterOnWorld -> {
                // Центрируемся на произвольной точке мира (0.5, 0.5)
                mapState.scrollTo(0.5, 0.5, destScale = 1.0)
                flowOf()
            }

            MapAction.ClickOnMapArea -> {
                val event = if (currentState.isMapAreaActive) {
                    HideMapArea
                } else {
                    ShowMapArea
                }
                flowOf(event)
            }

            is MapAction.AddPointToPath -> {
                if (pathDataPoints[pathId] == null) {
                    pathDataPoints[pathId] = mutableListOf((Pair(action.x, action.y)))
                } else {
                    pathDataPoints[pathId]?.add((Pair(action.x, action.y)))
                }

                showPath(mapState)
                if (pathDataPoints[pathId]?.size == 1) {
                    flowOf(FirstPointInPathEvent(action.x, action.y))
                } else {
                    flowOf()
                }
            }

            MapAction.HideSaveAreaDialog -> {
                flowOf(
                    DialogStateUpdated(
                        currentState.saveAreaDialogState.copy(
                            isVisible = false,
                            filterName = "",
                            isNameValid = true,
                            errorMessage = null
                        )
                    )
                )
            }
            MapAction.SaveArea -> {
                flowOf(
                    DialogStateUpdated(
                        currentState.saveAreaDialogState.copy(
                            isVisible = false,
                            filterName = "",
                            isNameValid = true,
                            errorMessage = null
                        )
                    )
                )
            }
            MapAction.ShowSaveAreaDialog -> {
                flowOf(
                    DialogStateUpdated(
                        currentState.saveAreaDialogState.copy(isVisible = true)
                    )
                )
            }
            is MapAction.UpdateAreaName -> {
                val isNameValid = action.name.length <= 25 && action.name.isNotBlank()
                val errorMessage = when {
                    action.name.isBlank() -> "Название области не может быть пустым"
                    action.name.length > 25 -> "Название области не должно превышать 25 символов"
                    else -> null
                }
                flowOf(
                    DialogStateUpdated(
                        currentState.saveAreaDialogState.copy(
                            filterName = action.name,
                            isNameValid = isNameValid,
                            errorMessage = errorMessage
                        )
                    )
                )
            }
        }
    }

    override suspend fun reduce(event: MapEvent, currentState: MapUiState): MapUiState =
        when (event) {
            Ready -> currentState
            HideMapArea -> currentState.copy(isMapAreaActive = false)
            ShowMapArea -> currentState.copy(isMapAreaActive = true)
            is FirstPointInPathEvent -> currentState
            is DialogStateUpdated -> {
                currentState.copy(saveAreaDialogState = event.dialogState)
            }
        }

    override suspend fun onEvent(event: MapEvent): MapEffect? {
        return when (event) {
            is FirstPointInPathEvent -> MapEffect.FirstPointInPathEffect(event.x, event.y)
            else -> super.onEvent(event)
        }
    }

    private fun showPath(mapState: MapState) {
        val pathBuilder = mapState.makePathDataBuilder().apply {
            pathDataPoints[pathId]?.forEach {
                this.addPoint(it.first, it.second)
            }
        }
        val pathData = pathBuilder.build()
        println("pathData $pathData")
        if (mapState.hasPath(pathId)) {
            mapState.removePath(pathId)
        }
        pathData?.let {
            mapState.addPath(
                id = pathId,
                pathData = pathData
            )
        }
    }
}

@Immutable
data class MapUiState(
    val isMapAreaActive: Boolean = false,
    val saveAreaDialogState: SaveDialogState = SaveDialogState()
) : MviState

sealed interface MapAction : MviAction {
    data object Initialize : MapAction
    data object ClickOnMapArea : MapAction
    data object CenterOnWorld : MapAction
    data class AddPointToPath(val x: Double, val y: Double) : MapAction

    data object ShowSaveAreaDialog : MapAction
    data object HideSaveAreaDialog : MapAction
    data class UpdateAreaName(val name: String) : MapAction
    data object SaveArea : MapAction
}

sealed interface MapEvent : MviEvent {
    data object Ready : MapEvent
    data object ShowMapArea : MapEvent
    data object HideMapArea : MapEvent
    data class FirstPointInPathEvent(val x: Double, val y: Double) : MapEvent
    data class DialogStateUpdated(val dialogState: SaveDialogState) : MapEvent
}

sealed interface MapEffect : MviEffect {
    data class FirstPointInPathEffect(val x: Double, val y: Double) : MapEffect
}


