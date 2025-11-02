package io.flatzen.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.utils.formatMainPrice
import io.flatzen.commoncomponents.utils.formatSecondPrice
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.viewmodel.MapAction
import io.flatzen.viewmodel.MapEffect
import io.flatzen.viewmodel.MapViewModel
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatListScreenAction
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.widgets.ActionButton
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.MapScreenWithFlatModalSheet
import io.flatzen.widgets.MessageSnackbar
import io.flatzen.widgets.dialogs.SaveDialog
import io.flatzen.widgets.dialogs.SavedAreasDialog
import io.flatzen.widgets.dialogs.SearchErrorDialog
import org.koin.compose.viewmodel.koinViewModel
import ovh.plrapps.mapcompose.api.ExperimentalClusteringApi
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.getPathData
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalClusteringApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = koinViewModel(),
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateToFilters: () -> Unit,
    navigateBack: () -> Unit,
    selectedMarker: Long?,
) {
    val listViewModel = koinViewModel<FlatSearchViewModel>()
    val listState by listViewModel.state.collectAsStateWithLifecycle()
    var selectedFlatId by remember { mutableStateOf<Long?>(null) }
    val selectedFlat = selectedFlatId?.let { id ->
        listState.flatList.find { it.adId == id }
    }
    val clusterId = "default"
    val isMarkersSizeTooBig = listState.flatList.size >= 270
    val detailFlatId by remember { mutableStateOf(selectedMarker) }
    val detailFlat = detailFlatId?.let { id ->
        listState.flatList.find { it.adId == id }
    }

    val filterViewModel = koinViewModel<FilterViewModel>()
    val filterState by filterViewModel.state.collectAsStateWithLifecycle()

    val mapModelState by mapViewModel.state.collectAsStateWithLifecycle()

    val savePathCalloutId = "savePathCalloutId"

    BackHandler {
        navigateBack()
    }

    LaunchedEffect(filterState.filters.location) {
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

    LaunchedEffect(listState.flatList, mapModelState.isMapAreaActive) {
        if (mapModelState.isMapAreaActive) {
            mapViewModel.mapState.removeAllMarkers()
        } else {
            mapViewModel.mapState.removeCallout(savePathCalloutId)
            mapViewModel.mapState.apply {
                onMarkerClick { id, x, y ->
                    selectedFlatId = id.toLongOrNull()
                }
                if (listState.flatList.isNotEmpty() && isMarkersSizeTooBig.not()) {
                    removeAllMarkers()
                    listState.flatList.forEach {
                        val mercatorCoordinates =
                            it.coordinates?.let { lonLatToNormalized(it.latitude, it.longitude) }
                                ?: return@LaunchedEffect
                        addMarker(
                            id = it.adId.toString(),
                            x = mercatorCoordinates.first,
                            y = mercatorCoordinates.second,
                            clickableAreaScale = Offset(1.3f, 1.3f),
                            renderingStrategy = RenderingStrategy.Clustering(clusterId)
                        ) {
                            RoomMarker(
                                rooms = it.numberOfRooms?.toIntOrNull(),
                                pinColor = if (detailFlatId == it.adId) Color.Green else Color(
                                    0xFFD32F2F
                                ),
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

    LaunchedEffect(mapModelState.isMapAreaActive) {
        mapViewModel.mapState.onTap { x, y ->
            if (mapModelState.isMapAreaActive) {
                mapViewModel.onIntent(MapAction.AddPointToPath(x, y))
            }
        }
    }

    LaunchedEffect(detailFlat) {
        mapViewModel.mapState.apply {
            if (selectedMarker != null) {
                val mercatorCoordinates = detailFlat?.coordinates?.let {
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

    LaunchedEffectOnce(Unit) { mapViewModel.onIntent(MapAction.Initialize) }
    LaunchedEffect(Unit) {
        listViewModel.onIntent(FlatListScreenAction.ScreenVisible)
    }
    LaunchedEffect(Unit) {
        mapViewModel.effect.collect {
            when (it) {
                is MapEffect.FirstPointInPathEffect -> {
                    mapViewModel.mapState.apply {
                        addMarker(
                            id = "pathid1",
                            x = it.x,
                            y = it.y,
                            clickableAreaScale = Offset(1.3f, 1.3f),
                        ) {
                            RoomMarker(
                                rooms = null,
                                pinColor = Color.Blue,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        onMarkerClick { id, x, y ->
                            mapViewModel.mapState.getPathData(it.pathId)?.size?.let { size ->
                                if (size > 2) {
                                    mapViewModel.onIntent(MapAction.AddPointToPath(x, y))
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
                                                    mapViewModel.onIntent(MapAction.ShowSaveAreaDialog)
                                                }
                                                .background(Color.White)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "Сохранить",
                                                color = Color.Black,
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
                }
            }
        }
    }

    if (mapModelState.saveAreaDialogState.isVisible) {
        SaveDialog(
            title = "Сохранить область",
            textFieldHint = "Название области",
            validationHint = "Название области не должно превышать 25 символов",
            dialogState = mapModelState.saveAreaDialogState,
            onNameChange = { name ->
                mapViewModel.onIntent(MapAction.UpdateAreaName(name))
            },
            onSave = {
                mapViewModel.onIntent(MapAction.SaveArea)
            },
            onCancel = {
                mapViewModel.onIntent(MapAction.HideSaveAreaDialog)
            }
        )
    }

    if (mapModelState.savedAreasDialogState.isVisible) {
        SavedAreasDialog(
            state = mapModelState.savedAreasDialogState,
            onCheckArea = { id, isChecked ->
                mapViewModel.onIntent(MapAction.ActivateMapArea(id, isChecked))
            },
            onDeleteArea = {
                mapViewModel.onIntent(MapAction.DeleteMapArea(it))
            },
            onDismiss = {
                mapViewModel.onIntent(MapAction.HideSavedAreaListDialog)
            }
        )
    }

    MapScreenWithFlatModalSheet(
        selectedFlat = selectedFlat,
        onFlatSelected = { flat ->
            selectedFlatId = flat?.adId
        },
        clickOnFavorite = {
            listViewModel.onIntent(
                FlatListScreenAction.ClickOnFavorite(
                    it.flatPlatform,
                    it.adId
                )
            )
        },
        navigateToDetails = navigateToDetails
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Text(
                            "Карта",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                )
            },
            floatingActionButton = {
                FilterActionButton(
                    onClick = navigateToFilters,
                    isAnyFilterApplied = listState.isAnyFilterApplied
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                MapUI(modifier = Modifier.fillMaxSize(), state = mapViewModel.mapState)
                if (listState.isLoading || listState.isLoadingMore) {
                    LinearProgressIndicator(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(12.dp)
                            .padding(4.dp)
                    )
                }

                if (listState.errorDialogState?.isVisible == true) {
                    SearchErrorDialog(
                        dialogState = listState.errorDialogState!!,
                        onDismiss = {
                            listViewModel.onIntent(FlatListScreenAction.HideNetworkErrorDialog)
                        }
                    )
                }

                if (mapModelState.isMapAreaActive) {
                    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
                        MessageSnackbar(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            message = "Кликай по карте, рисуя контур, замкни его нажав на первую точку\nЧтобы сохранить нажми на маркер",
                            color = Color(0xFF2b64ad).copy(alpha = 0.8f)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 56.dp + 6.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            ActionButton("Отменить") {
                                mapViewModel.onIntent(MapAction.UndoLastPoint)
                            }
                            Spacer(Modifier.width(8.dp))
                            ActionButton("Выход") {
                                mapViewModel.onIntent(MapAction.ClickOnMapArea)
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
                                    model = Res.getUri("drawable/area_on_map.png"),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable {
                                            mapViewModel.onIntent(MapAction.ClickOnMapArea)
                                        }
                                )
                                Spacer(Modifier.height(10.dp))
                                AsyncImage(
                                    model = Res.getUri("drawable/save_areas.svg"),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(32.dp)
                                        .clickable {
                                            mapViewModel.onIntent(MapAction.ShowSavedAreaListDialog)
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
                                        listViewModel.onIntent(
                                            FlatListScreenAction.SearchFlats(
                                                isLoadMore = false,
                                                isRefreshing = true
                                            )
                                        )
                                    }) {
                                    Text("Обновить")
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    enabled = listState.noFlatsToLoadMore.not() && isMarkersSizeTooBig.not(),
                                    colors = ButtonDefaults.buttonColors().copy(
                                        disabledContainerColor = ButtonDefaults.buttonColors().containerColor.copy(
                                            alpha = 0.7f
                                        ),
                                        disabledContentColor = ButtonDefaults.buttonColors().contentColor.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    onClick = {
                                        listViewModel.onIntent(
                                            FlatListScreenAction.SearchFlats(true)
                                        )
                                    }) {
                                    Text("Загрузить больше")
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
                                text = "Квартиры с текущими фильтрами закончились",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                        if (isMarkersSizeTooBig) {
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = "Слишком много объектов на карте, отображена только часть," +
                                        " добавьте фильтры или нажмите кнопку обновить",
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

@Composable
private fun Cluster(size: Int) {
    /* Here we can customize the cluster style */
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
    rooms: Int?,             // можно передавать "3" или "Ст"
    pinColor: Color = Color(0xFFD32F2F),  // насыщенный красный
    fillColor: Color = Color.White,       // белая внутренняя часть круга
    textColor: Color = Color(0xFF757575), // серый текст как на примере
    borderWidth: Dp = 2.dp,
    circleSize: Dp = 16.dp,
    tailWidth: Dp = 6.dp,
    tailHeight: Dp = 4.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Круг: белая заливка + красная обводка
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(fillColor)
                .border(borderWidth, pinColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            rooms?.let {
                Text(
                    text = rooms.toString(),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }

        // Треугольный хвостик (залит красным)
        Canvas(
            modifier = Modifier.size(width = tailWidth, height = tailHeight)
        ) {
            val path = Path().apply {
                // верхняя центральная точка (примыкает к кругу)
                moveTo(size.width / 2f, 0f)
                // левый нижний
                lineTo(0f, size.height)
                // правый нижний
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
    clickOnFavorite: () -> Unit
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
            clickOnFavorite = clickOnFavorite
        )

        Spacer(Modifier.height(8.dp))

        // Цена
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bynMainPrice = flat.priceUsd == null && flat.priceByn != null
            val mainPriceText = if (bynMainPrice) {
                formatMainPrice(flat.priceByn, "BYN")
            } else if (flat.priceUsd != null) {
                formatMainPrice(flat.priceUsd)
            } else "Цена не указана"

            val secondPriceText = if (flat.adType != AdType.DAILY && !bynMainPrice) {
                formatSecondPrice(flat.priceByn, mainPriceText != null)
            } else null
            if (mainPriceText != null) {
                Text(
                    text = mainPriceText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (secondPriceText != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = secondPriceText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
        }

        // Дата публикации, если есть
        flat.publishedAt?.let { date ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        val propertyTypeName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
        val propertyTypeRoom = flat.commercialUiInfo?.numberOfRooms
        if (propertyTypeName.isNullOrEmpty().not()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = propertyTypeName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val roomSuffix = if (propertyTypeRoom.isNullOrEmpty().not()) {
            "помещений"
        } else {
            "комн"
        }
        // Комнаты
        Text(
            text = "${flat.numberOfRooms} $roomSuffix",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

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