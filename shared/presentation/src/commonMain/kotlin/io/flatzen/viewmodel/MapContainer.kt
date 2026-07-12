package io.flatzen.viewmodel

import entities.UserMapArea
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.monetization.tier.UserTier
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.utils.normalizedToLonLat
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
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.fillter.FilterRepository
import repository.fillter.UserMapAreaRepository
import repository.fillter.areasInFilter
import kotlin.math.pow
import kotlin.random.Random
import ovh.plrapps.mapcompose.ui.state.MapState as MapComposeState

private typealias MapCtx = PipelineContext<MapUiState, MapIntent, MapAction>

class MapContainer(
    private val tileStreamProvider: TileStreamProvider,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val filterRepository: FilterRepository,
    private val userTierProvider: UserTierProvider,
    private val navigator: FlatHubNavigator,
) : Container<MapUiState, MapIntent, MapAction> {

    private val maxLevel = 18
    private val minLevel = 12
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    private val minsk = lonLatToNormalized(53.902147, 27.561388)
    private val x = minsk.first
    private val y = minsk.second

    val mapState = MapComposeState(levelCount = maxLevel + 1, mapSize, mapSize) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
        scroll(x, y)
        scale(0.0)
    }.apply {
        addLayer(tileStreamProvider)
        enableRotation()
    }

    private val pathDataPoints: MutableMap<String, MutableList<Pair<Double, Double>>> =
        mutableMapOf()
    private var currentPathId: String? = null

    override val store = store<MapUiState, MapIntent, MapAction>(
        initial = MapUiState.Initial
    ) {
        whileSubscribed {
            filterRepository.cashedFilterFlow.collect { _ ->
                val areaInFilter = filterRepository.areasInFilter(userMapAreaRepository)
                showPathFromDb(areaInFilter)
            }
        }
        reduce { intent ->
            when (intent) {
                MapIntent.Initialize -> { /* no state change */
                }

                MapIntent.CenterOnWorld -> {
                    mapState.scrollTo(0.5, 0.5, destScale = 1.0)
                }

                MapIntent.ClickOnMapArea -> handleClickOnMapArea()
                is MapIntent.AddPointToPath -> handleAddPointToPath(intent)
                MapIntent.UndoLastPoint -> handleUndoLastPoint()
                MapIntent.HideSaveAreaDialog -> updateState {
                    copy(
                        saveAreaDialogState = saveAreaDialogState.copy(
                            isVisible = false,
                            filterName = "",
                            isNameValid = true,
                            errorMessage = null
                        )
                    )
                }

                MapIntent.SaveArea -> handleSaveArea()
                MapIntent.ShowSaveAreaDialog -> updateState {
                    copy(saveAreaDialogState = saveAreaDialogState.copy(isVisible = true))
                }

                is MapIntent.UpdateAreaName -> updateState {
                    val isNameValid = intent.name.length <= 25 && intent.name.isNotBlank()
                    val errorMessage = when {
                        intent.name.isBlank() -> LocalizationKeys.MAP_AREA_NAME_EMPTY_ERROR
                        intent.name.length > 25 -> LocalizationKeys.MAP_AREA_NAME_LENGTH_ERROR
                        else -> null
                    }
                    copy(
                        saveAreaDialogState = saveAreaDialogState.copy(
                            filterName = intent.name,
                            isNameValid = isNameValid,
                            errorMessage = errorMessage
                        )
                    )
                }

                is MapIntent.OpenDetail -> navigator.navigate(
                    FlatHubCommand.OpenDetail(intent.flatPlatform, intent.adId)
                )

                MapIntent.OpenFilter -> navigator.navigate(FlatHubCommand.OpenFilter)
                MapIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
                MapIntent.OpenPremium -> navigator.navigate(FlatHubCommand.OpenPremium)
                MapIntent.RequestMapAreaOrPremium -> {
                    if (userTierProvider.currentTier() == UserTier.PREMIUM) {
                        handleClickOnMapArea()
                    } else {
                        navigator.navigate(FlatHubCommand.OpenPremium)
                    }
                }
            }
        }
    }

    private suspend fun MapCtx.handleClickOnMapArea() {
        withState {
            if (isMapAreaActive) {
                val areaInFilter = filterRepository.areasInFilter(userMapAreaRepository)
                val isCurrentPathInDb = areaInFilter.find { it.pathId == currentPathId } != null
                if (isCurrentPathInDb.not()) {
                    currentPathId?.let { id -> mapState.removePath(id) }
                }
                updateState { copy(isMapAreaActive = false) }
            } else {
                currentPathId = generatePathId()
                updateState { copy(isMapAreaActive = true, undoBtnVisible = true) }
            }
        }
    }

    private suspend fun MapCtx.handleAddPointToPath(intent: MapIntent.AddPointToPath) {
        val pathId = currentPathId ?: return
        if (pathDataPoints[pathId] == null) {
            pathDataPoints[pathId] = mutableListOf(Pair(intent.x, intent.y))
        } else {
            pathDataPoints[pathId]?.add(Pair(intent.x, intent.y))
        }
        showPath(pathId)
        if (pathDataPoints[pathId]?.size == 1) {
            action(MapAction.FirstPointInPath(pathId, intent.x, intent.y))
        }
    }

    private suspend fun MapCtx.handleUndoLastPoint() {
        val pathId = currentPathId ?: return
        pathDataPoints[pathId]?.removeLastOrNull()
        if (pathDataPoints[pathId].isNullOrEmpty()) {
            if (mapState.hasPath(pathId)) mapState.removePath(pathId)
            pathDataPoints.remove(pathId)
        } else {
            showPath(pathId)
        }
    }

    private suspend fun MapCtx.handleSaveArea() {
        val pathId = currentPathId ?: return
        val pathData = pathDataPoints[pathId] ?: return
        if (pathData.size >= 3) {
            val coordinates = calculateCoordinatesFromPath(pathData)
            if (coordinates != null) {
                withState {
                    userMapAreaRepository.saveArea(
                        UserMapArea(
                            pathId = pathId,
                            coordinates = coordinates,
                            isActive = true,
                            name = saveAreaDialogState.filterName
                        )
                    )
                }
            }
        }
        action(MapAction.MapAreaSaved(pathId))
        updateState {
            copy(
                undoBtnVisible = false,
                saveAreaDialogState = saveAreaDialogState.copy(
                    isVisible = false,
                    filterName = "",
                    isNameValid = true,
                    errorMessage = null
                )
            )
        }
    }

    private fun showPath(pathId: String) {
        val pathBuilder = mapState.makePathDataBuilder().apply {
            pathDataPoints[pathId]?.forEach { addPoint(it.first, it.second) }
        }
        val pathData = pathBuilder.build()
        if (mapState.hasPath(pathId)) mapState.removePath(pathId)
        pathData?.let { mapState.addPath(id = pathId, pathData = it) }
    }

    private fun showPathFromDb(items: List<UserMapArea>) {
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

    private fun generatePathId(): String = "path_${Random.nextInt()}"

    private fun calculateCoordinatesFromPath(pathPoints: List<Pair<Double, Double>>): List<Coordinates>? {
        if (pathPoints.size < 3) return null
        return pathPoints.map { point ->
            val (lat, lon) = normalizedToLonLat(point.first, point.second)
            Coordinates(lat, lon)
        }
    }
}
