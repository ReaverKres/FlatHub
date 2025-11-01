package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import entities.MapAreas
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.utils.normalizedToLonLat
import io.flatzen.viewmodel.MapEvent.FirstPointInPathEvent
import io.flatzen.viewmodel.MapEvent.HideMapArea
import io.flatzen.viewmodel.MapEvent.Ready
import io.flatzen.viewmodel.MapEvent.SaveAreaDialogStateUpdated
import io.flatzen.viewmodel.MapEvent.ShowMapArea
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.MapAreasUi
import io.flatzen.viewmodel.filter.SaveDialogState
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState
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
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import kotlin.math.pow
import kotlin.random.Random

class MapViewModel(
    private val tileStreamProvider: TileStreamProvider,
    private val filterRepository: FilterRepository,
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

    // Support for multiple paths with auto-generated IDs
    private val pathDataPoints: MutableMap<String, MutableList<Pair<Double, Double>>> =
        mutableMapOf()
    private var currentPathId: String? = null

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
                    // Remove all paths when hiding map area
                    HideMapArea
                } else {
                    // Generate a new path ID when starting to draw
                    currentPathId = generatePathId()
                    ShowMapArea
                }
                flowOf(event)
            }

            is MapAction.AddPointToPath -> {
                val pathId = currentPathId ?: return flowOf()

                if (pathDataPoints[pathId] == null) {
                    pathDataPoints[pathId] = mutableListOf(Pair(action.x, action.y))
                } else {
                    pathDataPoints[pathId]?.add(Pair(action.x, action.y))
                }

                showPath(mapState, pathId)
                if (pathDataPoints[pathId]?.size == 1) {
                    flowOf(FirstPointInPathEvent(pathId, action.x, action.y))
                } else {
                    flowOf()
                }
            }

            MapAction.UndoLastPoint -> {
                val pathId = currentPathId ?: return flowOf()
                pathDataPoints[pathId]?.removeLastOrNull()

                // If path is now empty, remove it entirely
                if (pathDataPoints[pathId].isNullOrEmpty()) {
                    if (mapState.hasPath(pathId)) {
                        mapState.removePath(pathId)
                    }
                    pathDataPoints.remove(pathId)
                } else {
                    // Redraw the path with remaining points
                    showPath(mapState, pathId)
                }
                flowOf()
            }

            MapAction.HideSaveAreaDialog -> {
                flowOf(
                    SaveAreaDialogStateUpdated(
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
                val pathId = currentPathId ?: return flowOf()
                val pathData = pathDataPoints[pathId] ?: return flowOf()

                if (pathData.size >= 3) { // Need at least 3 points to form an area
                    // Convert path points to lat/long bounds
                    val coordinates = calculateCoordinatesFromPath(pathData)
                    if (coordinates != null) {
                        // Save the area bounds with the filter
                        saveMapAreaCoordinates(
                            pathId,
                            coordinates,
                            currentState.saveAreaDialogState.filterName
                        )
                    }
                }

                // Hide dialog and reset
                flowOf(
                    SaveAreaDialogStateUpdated(
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
                    SaveAreaDialogStateUpdated(
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
                    SaveAreaDialogStateUpdated(
                        currentState.saveAreaDialogState.copy(
                            filterName = action.name,
                            isNameValid = isNameValid,
                            errorMessage = errorMessage
                        )
                    )
                )
            }
            MapAction.ShowSavedAreaListDialog -> {
                val filter = filterRepository.lastFilter()
                flowOf(
                    MapEvent.SavedAreasDialogStateUpdated(
                        currentState.savedAreasDialogState.copy(
                            isVisible = true,
                            savedFilters = MapAreasUi.mapFromModelToUi(filter.mapAreas)
                        )
                    )
                )
            }
            MapAction.HideSavedAreaListDialog -> {
                flowOf(
                    MapEvent.SavedAreasDialogStateUpdated(
                        currentState.savedAreasDialogState.copy(isVisible = false)
                    )
                )
            }

            is MapAction.ActivateMapArea -> {
                flowOf()
            }
            is MapAction.DeleteMapArea -> {
                flowOf()
            }
        }
    }

    override suspend fun reduce(event: MapEvent, currentState: MapUiState): MapUiState =
        when (event) {
            Ready -> currentState
            HideMapArea -> currentState.copy(isMapAreaActive = false)
            ShowMapArea -> currentState.copy(isMapAreaActive = true)
            is FirstPointInPathEvent -> currentState
            is SaveAreaDialogStateUpdated -> {
                currentState.copy(saveAreaDialogState = event.dialogState)
            }
            is MapEvent.SavedAreasDialogStateUpdated -> {
                currentState.copy(savedAreasDialogState = event.dialogState)
            }
        }

    override suspend fun onEvent(event: MapEvent): MapEffect? {
        return when (event) {
            is FirstPointInPathEvent -> MapEffect.FirstPointInPathEffect(event.pathId, event.x, event.y)
            else -> super.onEvent(event)
        }
    }

    private fun showPath(mapState: MapState, pathId: String) {
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

    private fun removeAllPaths() {
        pathDataPoints.keys.forEach { pathId ->
            if (mapState.hasPath(pathId)) {
                mapState.removePath(pathId)
            }
        }
        pathDataPoints.clear()
        currentPathId = null
    }

    private fun generatePathId(): String {
        return "path_${Random.nextInt()}"
    }

    private fun calculateCoordinatesFromPath(pathPoints: List<Pair<Double, Double>>): List<Coordinates>? {
        if (pathPoints.size < 3) return null

        // Convert all points from normalized coordinates to lat/lng
        val coordinates = pathPoints.map { point ->
            val normalized = normalizedToLonLat(point.first, point.second)
            Coordinates(normalized.first, normalized.second)
        }

        return coordinates
    }

    private suspend fun saveMapAreaCoordinates(
        pathId: String,
        coordinates: List<Coordinates>,
        name: String
    ) {
        val currentFilter = filterRepository.lastFilter()
        val updatedAreas: MutableList<MapAreas> = currentFilter.mapAreas.toMutableList()
        updatedAreas.add(
            MapAreas(
                id = pathId,
                coordinates = coordinates,
                isActive = true,
                name = name
            )
        )

        val updatedFilter = currentFilter.copy(mapAreas = updatedAreas)

        filterRepository.saveFilter(name, updatedFilter)
    }
}

@Immutable
data class MapUiState(
    val isMapAreaActive: Boolean = false,
    val saveAreaDialogState: SaveDialogState = SaveDialogState(),
    val savedAreasDialogState: SavedAreasDialogState = SavedAreasDialogState()
) : MviState

sealed interface MapAction : MviAction {
    data object Initialize : MapAction
    data object ClickOnMapArea : MapAction
    data object CenterOnWorld : MapAction
    data class AddPointToPath(val x: Double, val y: Double) : MapAction
    data object UndoLastPoint : MapAction

    data object ShowSaveAreaDialog : MapAction
    data object HideSaveAreaDialog : MapAction
    data class UpdateAreaName(val name: String) : MapAction
    data object SaveArea : MapAction
    data object ShowSavedAreaListDialog : MapAction
    data object HideSavedAreaListDialog : MapAction

    data class ActivateMapArea(val id: String, val checked: Boolean) : MapAction
    data class DeleteMapArea(val id: String) : MapAction
}

sealed interface MapEvent : MviEvent {
    data object Ready : MapEvent
    data object ShowMapArea : MapEvent
    data object HideMapArea : MapEvent
    data class FirstPointInPathEvent(val pathId: String, val x: Double, val y: Double) : MapEvent
    data class SaveAreaDialogStateUpdated(val dialogState: SaveDialogState) : MapEvent
    data class SavedAreasDialogStateUpdated(val dialogState: SavedAreasDialogState) : MapEvent
}

sealed interface MapEffect : MviEffect {
    data class FirstPointInPathEffect(val pathId: String, val x: Double, val y: Double) : MapEffect
}