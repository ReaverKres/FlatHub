package io.flatzen.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import entities.MetroLine
import entities.MetroStationGeo
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.detail_agent
import flatzen.composeapp.generated.resources.detail_owner
import flatzen.composeapp.generated.resources.list_commercial_rooms_suffix
import flatzen.composeapp.generated.resources.list_load_more
import flatzen.composeapp.generated.resources.list_no_more_flats
import flatzen.composeapp.generated.resources.list_rooms_suffix
import flatzen.composeapp.generated.resources.map_area_name_hint
import flatzen.composeapp.generated.resources.map_area_name_validation
import flatzen.composeapp.generated.resources.map_draw_instructions
import flatzen.composeapp.generated.resources.map_exit
import flatzen.composeapp.generated.resources.map_refresh
import flatzen.composeapp.generated.resources.map_save_area_title
import flatzen.composeapp.generated.resources.map_too_many_objects
import flatzen.composeapp.generated.resources.map_undo
import flatzen.composeapp.generated.resources.save
import flatzen.composeapp.generated.resources.tab_map
import io.flatzen.common.localization.localizedArea
import io.flatzen.commoncomponents.DrawablePath
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.usesSquareFeet
import io.flatzen.di.container
import io.flatzen.themes.FlatHubTheme
import io.flatzen.themes.FlatHubTheme.dimens
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.viewmodel.MapAction
import io.flatzen.viewmodel.MapContainer
import io.flatzen.viewmodel.MapIntent
import io.flatzen.viewmodel.MapIntent.AddPointToPath
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.list.FlatListIntent
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.widgets.ActionButton
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.MapScreenWithFlatModalSheet
import io.flatzen.widgets.MessageSnackbar
import io.flatzen.widgets.PremiumUpsellInlineText
import io.flatzen.widgets.PriceInsightLabel
import io.flatzen.widgets.dialogs.SaveDialog
import io.flatzen.widgets.dialogs.SavedAreasDialog
import io.flatzen.widgets.dialogs.SearchErrorDialog
import io.flatzen.widgets.rememberPremiumUpsellState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import listing.core.CoordEnrichState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ovh.plrapps.mapcompose.api.ExperimentalClusteringApi
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.getPathData
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.referentialSnapshotFlow
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.updateMarkerVisibility
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import io.flatzen.common.localization.stringResource as localizedStringResource

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalClusteringApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun MapScreen(
    mapViewModel: MapContainer = container(),
    selectedMarker: Long?,
    selectedLatitude: Double? = null,
    selectedLongitude: Double? = null,
    selectedRooms: Int? = null,
) {
    val flatSearchContainer: FlatSearchContainer = koinInject()
    val listState by flatSearchContainer.store.subscribe { }
    val coordEnrichState: CoordEnrichState = koinInject()
    val coordsEnrichLoading by coordEnrichState.isLoading.collectAsState()
    val premiumUpsell = rememberPremiumUpsellState(
        navigateToPremium = { mapViewModel.store.intent(MapIntent.OpenPremium) },
    )
    var selectedFlatId by remember { mutableStateOf<Long?>(null) }
    val selectedFlat = selectedFlatId?.let { id ->
        listState.flatList.find { it.adId == id }
    }
    val clusterId = "default"
    val flatsForMap = remember(listState.flatList) {
        listState.flatList.take(MAX_MAP_MARKERS)
    }
    val isMarkersSizeTooBig = listState.flatList.size > MAX_MAP_MARKERS
    val detailFlatId = selectedMarker
    val detailFlat = detailFlatId?.let { id ->
        listState.flatList.find { it.adId == id }
    }
    val selectedMarkerCoordinates = selectedLatitude?.let { lat ->
        selectedLongitude?.let { lon -> Coordinates(lat, lon) }
    }
    val highlightedFlatCoordinates = detailFlat?.coordinates ?: selectedMarkerCoordinates

    val metroChrome = remember { MetroMapChromeState() }
    val metroStationPainter = rememberAsyncImagePainter(
        model = Res.getUri(DrawablePath.METRO_STATION.value),
    )
    val selectedAdIdState = remember { mutableStateOf<Long?>(null) }
    selectedAdIdState.value = detailFlatId
    val trackedFlatMarkerIds = remember { mutableStateOf(emptySet<String>()) }
    val trackedMetroMarkerIds = remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(mapViewModel.mapState) {
        mapViewModel.mapState.referentialSnapshotFlow()
            .map { snapshot ->
                (snapshot.scale >= METRO_ICON_MIN_SCALE) to (snapshot.scale >= METRO_LABEL_MIN_SCALE)
            }
            .distinctUntilChanged()
            .collect { (showIcon, showLabel) ->
                metroChrome.showIcon = showIcon
                metroChrome.showLabel = showLabel
            }
    }

    val filterContainer: FilterContainer = container()
    val filterState by filterContainer.store.subscribe { }
    val savePathCalloutId = "savePathCalloutId"
    val firstPointInPathMarker = "pathid1"
    val pathMarkerColor = MaterialTheme.colorScheme.secondary
    val pathMarkerTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val saveCalloutText = stringResource(Res.string.save)

    val mapModelState by mapViewModel.store.subscribe { action ->
        when (action) {
            is MapAction.FirstPointInPath -> {
                mapViewModel.mapState.addMarker(
                    id = firstPointInPathMarker,
                    x = action.x,
                    y = action.y,
                    clickableAreaScale = Offset(1.3f, 1.3f),
                ) {
                    RoomMarker(
                        rooms = null,
                        pinColor = pathMarkerColor,
                        textColor = pathMarkerTextColor
                    )
                }
                mapViewModel.mapState.onMarkerClick { id, x, y ->
                    mapViewModel.mapState.getPathData(action.pathId)?.size?.let { size ->
                        if (size > 2) {
                            mapViewModel.store.intent(AddPointToPath(x, y))
                            mapViewModel.mapState.addCallout(
                                id = savePathCalloutId,
                                x = x,
                                y = y,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                        .clickable {
                                            mapViewModel.store.intent(MapIntent.ShowSaveAreaDialog)
                                        }
                                        .background(Color.White)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = saveCalloutText,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is MapAction.MapAreaSaved -> {
                mapViewModel.mapState.removeCallout(savePathCalloutId)
                mapViewModel.mapState.removeMarker(firstPointInPathMarker)
                mapViewModel.store.intent(MapIntent.ClickOnMapArea)
                filterContainer.intent(
                    FilterScreenAction.ActivateMapArea(
                        id = action.pathId,
                        checked = true,
                        doNetworkCall = true,
                        savedAreasDialogIsVisible = false
                    )
                )
            }
        }
    }

    BackHandler {
        mapViewModel.store.intent(MapIntent.NavigateBack)
    }

    LaunchedEffect(filterState.filters.location, selectedMarker) {
        if (selectedMarker != null) return@LaunchedEffect
        val location = filterState.filters.location
        mapViewModel.mapState.apply {
            val mercatorCoordinates = location?.selectedCity?.coordinates?.let {
                lonLatToNormalized(it.latitude, it.longitude)
            } ?: return@LaunchedEffect
            scrollTo(
                x = mercatorCoordinates.first,
                y = mercatorCoordinates.second
            )
        }
    }

    LaunchedEffect(
        flatsForMap,
        mapModelState.isMapAreaActive,
        mapModelState.metroStations,
        selectedMarker,
        highlightedFlatCoordinates,
        selectedRooms,
    ) {
        if (mapModelState.isMapAreaActive) {
            mapViewModel.mapState.removeAllMarkers()
            trackedFlatMarkerIds.value = emptySet()
            trackedMetroMarkerIds.value = emptySet()
        } else {
            mapViewModel.mapState.removeCallout(savePathCalloutId)
            mapViewModel.mapState.apply {
                onMarkerClick { id, _, _ ->
                    selectedFlatId = id.toLongOrNull()
                }

                val desiredFlatIds = flatsForMap.map { it.adId.toString() }.toSet()
                val currentFlatIds = trackedFlatMarkerIds.value
                (currentFlatIds - desiredFlatIds).forEach { id -> removeMarker(id) }

                flatsForMap.forEach { flat ->
                    val id = flat.adId.toString()
                    if (id in currentFlatIds && hasMarker(id)) return@forEach
                    val mercatorCoordinates =
                        flat.coordinates?.let { lonLatToNormalized(it.latitude, it.longitude) }
                            ?: return@forEach
                    val adId = flat.adId
                    val rooms = flat.numberOfRooms?.toIntOrNull()
                    addMarker(
                        id = id,
                        x = mercatorCoordinates.first,
                        y = mercatorCoordinates.second,
                        clickableAreaScale = Offset(1.3f, 1.3f),
                        renderingStrategy = RenderingStrategy.Clustering(clusterId)
                    ) {
                        val selectedId = selectedAdIdState.value
                        RoomMarker(
                            rooms = rooms,
                            pinColor = if (selectedId == adId) Color.Green else Color(0xFFD32F2F),
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trackedFlatMarkerIds.value = desiredFlatIds

                if (detailFlatId != null && highlightedFlatCoordinates != null) {
                    val highlightId = detailFlatId.toString()
                    val flatInMarkersWithCoordinates = flatsForMap.any {
                        it.adId == detailFlatId && it.coordinates != null
                    }
                    if (!flatInMarkersWithCoordinates && !hasMarker(highlightId)) {
                        val mercatorCoordinates = lonLatToNormalized(
                            highlightedFlatCoordinates.latitude,
                            highlightedFlatCoordinates.longitude,
                        )
                        addMarker(
                            id = highlightId,
                            x = mercatorCoordinates.first,
                            y = mercatorCoordinates.second,
                            clickableAreaScale = Offset(1.3f, 1.3f),
                            renderingStrategy = RenderingStrategy.Clustering(clusterId)
                        ) {
                            RoomMarker(
                                rooms = selectedRooms
                                    ?: detailFlat?.numberOfRooms?.toIntOrNull(),
                                pinColor = Color.Green,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val desiredMetroIds = mapModelState.metroStations
                    .map { "$METRO_MARKER_ID_PREFIX${it.metroId}" }
                    .toSet()
                if (trackedMetroMarkerIds.value != desiredMetroIds) {
                    (trackedMetroMarkerIds.value - desiredMetroIds).forEach { removeMarker(it) }
                    addMetroStationMarkers(
                        stations = mapModelState.metroStations.filter {
                            "$METRO_MARKER_ID_PREFIX${it.metroId}" !in trackedMetroMarkerIds.value
                        },
                        chrome = metroChrome,
                        iconPainter = metroStationPainter,
                    )
                    trackedMetroMarkerIds.value = desiredMetroIds
                }
                desiredMetroIds.forEach { id ->
                    updateMarkerVisibility(id, metroChrome.showIcon)
                }

                if (flatsForMap.isNotEmpty()) {
                    addClusterer(
                        id = clusterId,
                        clusteringThreshold = 30.dp
                    ) { ids ->
                        {
                            Cluster(size = ids.size)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(metroChrome.showIcon, trackedMetroMarkerIds.value) {
        trackedMetroMarkerIds.value.forEach { id ->
            mapViewModel.mapState.updateMarkerVisibility(id, metroChrome.showIcon)
        }
    }

    LaunchedEffect(mapModelState.isMapAreaActive) {
        mapViewModel.mapState.onTap { x, y ->
            if (mapModelState.isMapAreaActive) {
                mapViewModel.store.intent(MapIntent.AddPointToPath(x, y))
            }
        }
    }

    LaunchedEffect(selectedMarker, highlightedFlatCoordinates) {
        mapViewModel.mapState.apply {
            if (selectedMarker != null) {
                val mercatorCoordinates = highlightedFlatCoordinates?.let {
                    lonLatToNormalized(it.latitude, it.longitude)
                } ?: return@LaunchedEffect
                scrollTo(
                    x = mercatorCoordinates.first,
                    y = mercatorCoordinates.second,
                    destScale = scale * 20 // Adjust this value for desired zoom level
                )
            }
        }
    }

    LaunchedEffectOnce(Unit) { mapViewModel.store.intent(MapIntent.Initialize) }
    LaunchedEffect(Unit) {
        flatSearchContainer.store.intent(FlatListIntent.ScreenVisible)
    }

    var isResumed by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isResumed = true }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { isResumed = false }

    if (mapModelState.saveAreaDialogState.isVisible) {
        SaveDialog(
            title = stringResource(Res.string.map_save_area_title),
            textFieldHint = stringResource(Res.string.map_area_name_hint),
            validationHint = stringResource(Res.string.map_area_name_validation),
            dialogState = mapModelState.saveAreaDialogState,
            onNameChange = { name ->
                mapViewModel.store.intent(MapIntent.UpdateAreaName(name))
            },
            onSave = {
                mapViewModel.store.intent(MapIntent.SaveArea)
            },
            onCancel = {
                mapViewModel.store.intent(MapIntent.HideSaveAreaDialog)
            }
        )
    }

    if (filterState.savedAreasDialogState.isVisible) {
        SavedAreasDialog(
            state = filterState.savedAreasDialogState,
            onCheckArea = { id, isChecked ->
                filterContainer.intent(FilterScreenAction.ActivateMapArea(id, isChecked, true))
            },
            onDeleteArea = {
                filterContainer.intent(FilterScreenAction.DeleteMapArea(it, true))
            },
            onDismiss = {
                filterContainer.intent(FilterScreenAction.HideSavedAreaListDialog)
            }
        )
    }

    MapScreenWithFlatModalSheet(
        selectedFlat = selectedFlat,
        onFlatSelected = { flat ->
            selectedFlatId = flat?.adId
        },
        clickOnFavorite = {
            flatSearchContainer.intent(
                FlatListIntent.ClickOnFavorite(
                    it.flatPlatform,
                    it.adId
                )
            )
        },
        clickOnClearDislike = {
            flatSearchContainer.intent(
                FlatListIntent.ClearDislike(
                    it.flatPlatform,
                    it.adId
                )
            )
        },
        navigateToDetails = { platform, adId ->
            mapViewModel.store.intent(MapIntent.OpenDetail(platform, adId))
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.then(
                        if (premiumUpsell != null) {
                            Modifier.background(FlatHubTheme.semantic.premiumDelayHint)
                        } else {
                            Modifier
                        }
                    ),
                    navigationIcon = {
                        if (selectedMarker != null) {
                            IconButton(onClick = {
                                mapViewModel.store.intent(MapIntent.NavigateBack)
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.back),
                                )
                            }
                        }
                    },
                    title = {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(Res.string.tab_map),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (premiumUpsell != null) {
                                    FlatHubTheme.semantic.onPremiumDelayHint
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (premiumUpsell != null) {
                                Spacer(Modifier.width(dimens.horizontalSpacing6))
                                PremiumUpsellInlineText(
                                    state = premiumUpsell,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                FilterActionButton(
                    onClick = { mapViewModel.store.intent(MapIntent.OpenFilter) },
                    isAnyFilterApplied = listState.isAnyFilterApplied
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
                MapUI(
                    modifier = Modifier.fillMaxSize(),
                    state = mapViewModel.mapState
                )
                if (listState.isLoading || listState.isLoadingMore || coordsEnrichLoading) {
                    LinearProgressIndicator(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(12.dp)
                            .padding(4.dp)
                    )
                }

                if (isResumed && listState.errorDialogState?.isVisible == true) {
                    SearchErrorDialog(
                        dialogState = listState.errorDialogState!!,
                        onDismiss = { dontShowAgain ->
                            flatSearchContainer.intent(
                                FlatListIntent.HideNetworkErrorDialog(dontShowAgain)
                            )
                        }
                    )
                }

                if (mapModelState.isMapAreaActive) {
                        MessageSnackbar(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            message = stringResource(Res.string.map_draw_instructions),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 56.dp + 6.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            if (mapModelState.undoBtnVisible) {
                                ActionButton(stringResource(Res.string.map_undo)) {
                                    mapViewModel.store.intent(MapIntent.UndoLastPoint)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            ActionButton(stringResource(Res.string.map_exit)) {
                                mapViewModel.store.intent(MapIntent.ClickOnMapArea)
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            // Картинки слева
                            Column(
                                modifier = Modifier.align(Alignment.TopStart)
                                    .padding(start = 16.dp)
                            ) {
                                AsyncImage(
                                    model = Res.getUri(DrawablePath.AREA_ON_MAP.value),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable {
                                            mapViewModel.store.intent(MapIntent.ClickOnMapArea)
                                        }
                                )
                                Spacer(Modifier.height(10.dp))
                                AsyncImage(
                                    model = Res.getUri(DrawablePath.SAVE_AREAS.value),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(32.dp)
                                        .clickable {
                                            filterContainer.intent(FilterScreenAction.ShowSavedAreaListDialog)
                                        }
                                )
                            }

                            // Кнопки по центру
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    onClick = {
                                        flatSearchContainer.intent(
                                            FlatListIntent.SearchFlats(
                                                isLoadMore = false,
                                                isRefreshing = true
                                            )
                                        )
                                    }) {
                                    Text(stringResource(Res.string.map_refresh))
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    enabled = isMarkersSizeTooBig.not(),
                                    colors = ButtonDefaults.buttonColors().copy(
                                        disabledContainerColor = ButtonDefaults.buttonColors().containerColor.copy(
                                            alpha = 0.7f
                                        ),
                                        disabledContentColor = ButtonDefaults.buttonColors().contentColor.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    onClick = {
                                        flatSearchContainer.intent(
                                            FlatListIntent.SearchFlats(
                                                isLoadMore = true,
                                                isLoadMoreForce = true
                                            )
                                        )
                                    }) {
                                    Text(stringResource(Res.string.list_load_more))
                                }
                            }

                            // Пустой элемент справа для балансировки
                            Spacer(
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 56.dp + 6.dp)
                    ) {
                        if (listState.noFlatsToLoadMore) {
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = stringResource(Res.string.list_no_more_flats),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                        if (isMarkersSizeTooBig) {
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = stringResource(Res.string.map_too_many_objects),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val METRO_MARKER_ID_PREFIX = "metro_"
private const val MAX_MAP_MARKERS = 270
private const val METRO_ICON_MIN_SCALE = 0.08
private const val METRO_LABEL_MIN_SCALE = 0.25

@Stable
private class MetroMapChromeState {
    var showIcon by mutableStateOf(false)
    var showLabel by mutableStateOf(false)
}

private fun MetroLine.toMapColor(): Color = when (this) {
    MetroLine.RED -> Color(0xFFBC6B66)
    MetroLine.BLUE -> Color(0xFF6E8FAD)
    MetroLine.GREEN -> Color(0xFF729E78)
    MetroLine.BAKERLOO -> Color(0xFFB36305)
    MetroLine.CENTRAL -> Color(0xFFE32017)
    MetroLine.CIRCLE -> Color(0xFFFFD300)
    MetroLine.DISTRICT -> Color(0xFF00782A)
    MetroLine.HAMMERSMITH_CITY -> Color(0xFFF3A9BB)
    MetroLine.JUBILEE -> Color(0xFFA0A5A9)
    MetroLine.METROPOLITAN -> Color(0xFF9B0056)
    MetroLine.NORTHERN -> Color(0xFF000000)
    MetroLine.PICCADILLY -> Color(0xFF003688)
    MetroLine.VICTORIA -> Color(0xFF0098D4)
    MetroLine.WATERLOO_CITY -> Color(0xFF95CDBA)
    MetroLine.ELIZABETH -> Color(0xFF6950A1)
    MetroLine.PARIS_M1 -> Color(0xFFFFCD00)
    MetroLine.PARIS_M2 -> Color(0xFF003CA6)
    MetroLine.PARIS_M3 -> Color(0xFF837902)
    MetroLine.PARIS_M4 -> Color(0xFFCF009E)
    MetroLine.PARIS_M5 -> Color(0xFFFF7E2E)
    MetroLine.PARIS_M6 -> Color(0xFF6ECA97)
    MetroLine.PARIS_M7 -> Color(0xFFFA9ABA)
    MetroLine.PARIS_M8 -> Color(0xFFE19BDF)
    MetroLine.PARIS_M9 -> Color(0xFFB6BD00)
    MetroLine.PARIS_M10 -> Color(0xFFC9910D)
    MetroLine.PARIS_M11 -> Color(0xFF704B1C)
    MetroLine.PARIS_M12 -> Color(0xFF007852)
    MetroLine.PARIS_M13 -> Color(0xFF6EC4E8)
    MetroLine.PARIS_M14 -> Color(0xFF62259D)
    MetroLine.PARIS_RER_A -> Color(0xFFF7403A)
    MetroLine.PARIS_RER_B -> Color(0xFF5291CE)
    MetroLine.TORONTO_L1 -> Color(0xFFFFCC00)
    MetroLine.TORONTO_L2 -> Color(0xFF008000)
    MetroLine.TORONTO_L3 -> Color(0xFF0080FF)
    MetroLine.TORONTO_L4 -> Color(0xFF9D2089)
    MetroLine.WIEN_U1 -> Color(0xFFE20613)
    MetroLine.WIEN_U2 -> Color(0xFFA862A4)
    MetroLine.WIEN_U3 -> Color(0xFFE8702A)
    MetroLine.WIEN_U4 -> Color(0xFF00963F)
    MetroLine.WIEN_U6 -> Color(0xFF9D6910)
    MetroLine.SEOUL_1 -> Color(0xFF0052A4)
    MetroLine.SEOUL_2 -> Color(0xFF00A84D)
    MetroLine.SEOUL_3 -> Color(0xFFEF7C1C)
    MetroLine.SEOUL_4 -> Color(0xFF00A5DE)
    MetroLine.SEOUL_5 -> Color(0xFF996CAC)
    MetroLine.SEOUL_6 -> Color(0xFFCD7C2F)
    MetroLine.SEOUL_7 -> Color(0xFF747F00)
    MetroLine.SEOUL_8 -> Color(0xFFE6186C)
    MetroLine.SEOUL_9 -> Color(0xFFBDB092)
    MetroLine.BERLIN_U1 -> Color(0xFF7DAD4C)
    MetroLine.BERLIN_U2 -> Color(0xFFDA421E)
    MetroLine.BERLIN_U3 -> Color(0xFF16683D)
    MetroLine.BERLIN_U4 -> Color(0xFFF0D722)
    MetroLine.BERLIN_U5 -> Color(0xFF7E5330)
    MetroLine.BERLIN_U6 -> Color(0xFF8C6DAB)
    MetroLine.BERLIN_U7 -> Color(0xFF528DCA)
    MetroLine.BERLIN_U8 -> Color(0xFF224F86)
    MetroLine.BERLIN_U9 -> Color(0xFFF3791D)
    MetroLine.BERLIN_S1 -> Color(0xFFDE4DA4)
    MetroLine.BERLIN_S2 -> Color(0xFF007734)
    MetroLine.BERLIN_S3 -> Color(0xFF0066A6)
    MetroLine.BERLIN_S5 -> Color(0xFFF29EC3)
    MetroLine.BERLIN_S7 -> Color(0xFF816DA6)
    MetroLine.BERLIN_S9 -> Color(0xFF992746)
    MetroLine.MADRID_1 -> Color(0xFF29B0E0)
    MetroLine.MADRID_2 -> Color(0xFFC4002D)
    MetroLine.MADRID_3 -> Color(0xFFFFD100)
    MetroLine.MADRID_4 -> Color(0xFFA05A2C)
    MetroLine.MADRID_5 -> Color(0xFF8EC63F)
    MetroLine.MADRID_6 -> Color(0xFF999999)
    MetroLine.MADRID_7 -> Color(0xFFF07A25)
    MetroLine.MADRID_8 -> Color(0xFFEB82BD)
    MetroLine.MADRID_9 -> Color(0xFFA61C4D)
    MetroLine.MADRID_10 -> Color(0xFF005CA9)
    MetroLine.MADRID_11 -> Color(0xFF009640)
    MetroLine.MADRID_12 -> Color(0xFFA49700)
    MetroLine.MADRID_R -> Color(0xFFCCCCCC)
    MetroLine.BCN_L1 -> Color(0xFFCE1126)
    MetroLine.BCN_L2 -> Color(0xFF7B2D8E)
    MetroLine.BCN_L3 -> Color(0xFF00A650)
    MetroLine.BCN_L4 -> Color(0xFFFAB914)
    MetroLine.BCN_L5 -> Color(0xFF0077C8)
    MetroLine.BCN_L9N -> Color(0xFFFF6B00)
    MetroLine.BCN_L9S -> Color(0xFFFF8C00)
    MetroLine.BCN_L10 -> Color(0xFF00A0E3)
    MetroLine.BCN_L11 -> Color(0xFF8DC63F)
    MetroLine.BKK_BTS_SUKHUMVIT -> Color(0xFF5BBF21)
    MetroLine.BKK_BTS_SILOM -> Color(0xFF008C45)
    MetroLine.BKK_MRT_BLUE -> Color(0xFF1E3A8A)
    MetroLine.BKK_MRT_PURPLE -> Color(0xFF6B21A8)
    MetroLine.BKK_ARL -> Color(0xFFC8102E)
    MetroLine.TOKYO_YAMANOTE -> Color(0xFF9ACD32)
    MetroLine.TOKYO_GINZA -> Color(0xFFFF9500)
    MetroLine.TOKYO_MARUNOUCHI -> Color(0xFFE60012)
    MetroLine.TOKYO_HIBIYA -> Color(0xFFB5B5AC)
    MetroLine.TOKYO_TOZAI -> Color(0xFF009BBF)
    MetroLine.TOKYO_CHIYODA -> Color(0xFF00BB85)
    MetroLine.TOKYO_YURAKUCHO -> Color(0xFFC1A470)
    MetroLine.TOKYO_HANZOMON -> Color(0xFF8F76D6)
    MetroLine.TOKYO_NAMBOKU -> Color(0xFF00ADA9)
    MetroLine.TOKYO_FUKUTOSHIN -> Color(0xFF9C5E31)
    MetroLine.TOKYO_ASAKUSA -> Color(0xFFE85298)
    MetroLine.TOKYO_MITA -> Color(0xFF0079C2)
    MetroLine.TOKYO_SHINJUKU -> Color(0xFF6CBB5A)
    MetroLine.TOKYO_OEDO -> Color(0xFFB6007A)
}

private fun formatMetroStationName(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size <= 1) return name
    val splitAt = words.size / 2
    return words.take(splitAt).joinToString(" ") + "\n" + words.drop(splitAt).joinToString(" ")
}

private fun MapState.addMetroStationMarkers(
    stations: List<MetroStationGeo>,
    chrome: MetroMapChromeState,
    iconPainter: Painter,
) {
    stations.forEach { station ->
        val id = "$METRO_MARKER_ID_PREFIX${station.metroId}"
        if (hasMarker(id)) return@forEach
        val (x, y) = lonLatToNormalized(
            station.coordinates.latitude,
            station.coordinates.longitude,
        )
        val lineColor = station.line.toMapColor()
        val label = formatMetroStationName(station.canonicalName)
        addMarker(
            id = id,
            x = x,
            y = y,
        ) {
            MetroStationMapMarker(
                chrome = chrome,
                name = label,
                lineColor = lineColor,
                iconPainter = iconPainter,
            )
        }
        updateMarkerVisibility(id, chrome.showIcon)
    }
}

@Composable
private fun MetroStationMapMarker(
    chrome: MetroMapChromeState,
    name: String,
    lineColor: Color,
    iconPainter: Painter,
) {
    if (!chrome.showIcon) return

    Row(
        modifier = Modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(lineColor),
        )
        if (chrome.showLabel) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = lineColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun Cluster(size: Int) {
    Box(
        modifier = Modifier
            .background(
                color = Color.Yellow.copy(alpha = 0.65f),
                shape = CircleShape
            )
            .size(25.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = size.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RoomMarker(
    rooms: Int?,
    pinColor: Color = Color(0xFFD32F2F),
    fillColor: Color = Color.White,
    textColor: Color = Color(0xFF757575),
) {
    val circleSize = 16.dp
    val tailWidth = 6.dp
    val tailHeight = 4.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(fillColor, CircleShape)
                .border(2.dp, pinColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (rooms != null) {
                Text(
                    text = rooms.toString(),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        Canvas(modifier = Modifier.size(width = tailWidth, height = tailHeight)) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path = path, color = pinColor)
        }
    }
}

@Composable
fun FlatItemContent(
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit,
    clickOnClearDislike: () -> Unit = {},
) {
    // Вариация FlatCard для BottomSheet: компактная высота изображения, более плотные отступы
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        FlatImagePager(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            flatPlatform = flat.flatPlatform,
            imageUrls = flat.imageUrls,
            isViewed = flat.isViewed,
            savedInFavorite = flat.savedInFavorite,
            saveInFavoriteInProgress = flat.saveInFavoriteInProgress,
            disliked = flat.disliked,
            clickOnFavorite = clickOnFavorite,
            clickOnClearDislike = clickOnClearDislike,
        )

        Spacer(Modifier.height(8.dp))

        // Цена
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val mainPriceText = flat.priceText.mainPriceText
            val secondPriceText = flat.priceText.secondPriceText
            Text(
                text = mainPriceText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondPriceText != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = secondPriceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
        }

        PriceInsightLabel(
            priceVsAreaAvgPercent = flat.priceVsAreaAvgPercent,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )

        // Дата публикации, если есть
        flat.publishedAt?.let { date ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        flat.isOwner?.let { owner ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (owner) {
                    stringResource(Res.string.detail_owner)
                } else {
                    stringResource(Res.string.detail_agent)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        val propertyTypeName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
        val propertyTypeRoom = flat.commercialUiInfo?.numberOfRooms
        propertyTypeName?.let { name ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = localizedStringResource(name),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val roomSuffix = if (propertyTypeRoom.isNullOrEmpty().not()) {
            stringResource(Res.string.list_commercial_rooms_suffix)
        } else {
            stringResource(Res.string.list_rooms_suffix)
        }
        val hasRooms = !flat.numberOfRooms.isNullOrBlank()
        val hasArea = !flat.totalArea.isNullOrEmpty()
        if (hasRooms || hasArea) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (hasRooms) {
                    Text(
                        text = buildString {
                            append(flat.numberOfRooms)
                            append(' ')
                            append(roomSuffix)
                            if (hasArea) append(',')
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                if (hasArea) {
                    Text(
                        text = localizedArea(
                            flat.totalArea!!,
                            usesSquareFeet = flat.flatPlatform.usesSquareFeet(),
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Метро
        if (flat.metroStation?.isNotBlank() == true) {
            Text(
                text = flat.metroStation!!,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Адрес — до 3 строк, т.к. в шите больше места, но все же ограничим
        if (flat.address.isNotBlank()) {
            Text(
                text = flat.address,
                style = MaterialTheme.typography.bodySmall,
                minLines = 2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}