package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import entities.UserMapArea
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
import io.flatzen.viewmodel.filter.SaveDialogState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.hasPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import repository.fillter.FilterRepository
import repository.fillter.UserMapAreaRepository
import repository.fillter.areasInFilter
import kotlin.math.pow
import kotlin.random.Random

class MapViewModel(
    private val tileStreamProvider: TileStreamProvider,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val filterRepository: FilterRepository
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

    init {
        filterRepository.cashedFilterFlow
            .onEach { items ->
                val areaInFilter = filterRepository.areasInFilter(userMapAreaRepository)
                showPathFromDb(areaInFilter)
            }
//            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

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

                showPath(pathId)
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
                    showPath(pathId)
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
                    MapEvent.MapAreaSaved(pathId),
                    MapEvent.HideUndoBtn,
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
        }
    }

    override suspend fun reduce(event: MapEvent, currentState: MapUiState): MapUiState =
        when (event) {
            Ready -> currentState
            HideMapArea -> currentState.copy(isMapAreaActive = false)
            ShowMapArea -> currentState.copy(isMapAreaActive = true, undoBtnVisible = true)
            MapEvent.HideUndoBtn -> currentState.copy(undoBtnVisible = false)

            is SaveAreaDialogStateUpdated -> {
                currentState.copy(saveAreaDialogState = event.dialogState)
            }

            is FirstPointInPathEvent -> currentState
            is MapEvent.MapAreaSaved -> currentState
        }

    override suspend fun onEvent(event: MapEvent): MapEffect? {
        return when (event) {
            is FirstPointInPathEvent -> MapEffect.FirstPointInPathEffect(
                event.pathId,
                event.x,
                event.y
            )
            is MapEvent.MapAreaSaved -> MapEffect.MapAreaSavedEffect(event.pathId)
            else -> super.onEvent(event)
        }
    }

    private fun showPath(pathId: String) {
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

    private suspend fun showPathFromDb(items: List<UserMapArea>) {
        pathDataPoints.clear()
        mapState.removeAllPaths()
        items.forEach { mapAreaDb ->
            if (mapAreaDb.isActive) {
                val normalizedCoordinates = mapAreaDb.coordinates.map { coordinate ->
                    lonLatToNormalized(coordinate.latitude, coordinate.longitude)
                }.toMutableList()
                pathDataPoints[mapAreaDb.pathId] = normalizedCoordinates
                showPath(mapAreaDb.pathId)
            }
        }
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
        userMapAreaRepository.saveArea(
            UserMapArea(
                pathId = pathId,
                coordinates = coordinates,
                isActive = true,
                name = name
            )
        )
    }
}

@Immutable
data class MapUiState(
    val isMapAreaActive: Boolean = false,
    val undoBtnVisible: Boolean = true,
    val saveAreaDialogState: SaveDialogState = SaveDialogState(),
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
}

sealed interface MapEvent : MviEvent {
    data object Ready : MapEvent
    data object ShowMapArea : MapEvent
    data object HideMapArea : MapEvent
    data object HideUndoBtn: MapEvent
    data class FirstPointInPathEvent(val pathId: String, val x: Double, val y: Double) : MapEvent
    data class SaveAreaDialogStateUpdated(val dialogState: SaveDialogState) : MapEvent
    data class MapAreaSaved(val pathId: String) : MapEvent
}

sealed interface MapEffect : MviEffect {
    data class FirstPointInPathEffect(val pathId: String, val x: Double, val y: Double) : MapEffect
    data class MapAreaSavedEffect(val pathId: String) : MapEffect
}