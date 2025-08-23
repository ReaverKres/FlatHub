package io.flatzen.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.viewmodel.FlatListScreenAction
import io.flatzen.viewmodel.FlatSearchViewModel
import io.flatzen.viewmodel.MapAction
import io.flatzen.viewmodel.MapViewModel
import io.flatzen.viewmodel.UiFlat
import io.flatzen.widgets.AppAsyncImage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ovh.plrapps.mapcompose.api.ExperimentalClusteringApi
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.ui.MapUI
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalClusteringApi::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = koinViewModel(),
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateToFilters: () -> Unit,
) {
    val listViewModel = koinViewModel<FlatSearchViewModel>()
    val listState by listViewModel.state.collectAsStateWithLifecycle()
    var selectedFlat by remember { mutableStateOf<UiFlat?>(null) }

    mapViewModel.mapState.apply {
//        addClusterer("default") { ids ->
//            { Cluster(size = ids.size) }
//        }
        onMarkerClick { id, x, y ->
            val selectedAdId = id.toLongOrNull()
            val flat = selectedAdId?.let { targetId ->
                listState.flatList.firstOrNull { it.adId == targetId }
            }
            selectedFlat = flat
        }
        if (listState.flatList.isNotEmpty()) {
            removeAllMarkers()
            listState.flatList.forEach {
                val mercatorCoordinates = it.coordinates?.let { lonLatToNormalized(it.latitude, it.longitude) } ?: return
                addMarker(
                    id = it.adId.toString(),
                    x = mercatorCoordinates.first,
                    y = mercatorCoordinates.second,
                    clickableAreaScale = Offset(1.2f, 1.2f)
                    ) {
                    Icon(
                        Icons.Default.LocationOn,
                        tint = Color.Red,
                        contentDescription = null
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) { mapViewModel.onIntent(MapAction.Initialize) }
    LaunchedEffect(Unit) {
        listViewModel.onIntent(FlatListScreenAction.ScreenVisible)
    }

// Оборачиваем карту скэффолд с нижним шитом
    MapScreenWithFlatModalSheet(
        selectedFlat = selectedFlat,
        onFlatSelected = { selectedFlat = it },
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
            topBar = { TopAppBar(title = { Text("Карта", style = MaterialTheme.typography.titleLarge) }) },
            floatingActionButton = {
                Box {
                    FloatingActionButton(onClick = navigateToFilters) {
                        Icon(Icons.Default.Build, contentDescription = "Фильтры")
                    }

//                if (filterState.filters.isAnyFilterActive()) {
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .size(12.dp)
//                            .clip(CircleShape)
//                            .background(Color.Red)
//                    )
//                }
                }
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                MapUI(modifier = Modifier.fillMaxSize(), state = mapViewModel.mapState)
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
                Color.Red.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = size.toString(), color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenWithFlatModalSheet(
    selectedFlat: UiFlat?,
    onFlatSelected: (UiFlat?) -> Unit,
    clickOnFavorite: (UiFlat) -> Unit,
    navigateToDetails: (FlatPlatform, Long) -> Unit,
    mapContent: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val open = selectedFlat != null

    Box(Modifier.fillMaxSize()) {
        mapContent()

        if (open) {
            ModalBottomSheet(
                onDismissRequest = { onFlatSelected(null) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                selectedFlat?.let { flat ->
                    FlatBottomSheetContent(
                        flat = flat,
                        onClick = {
                            navigateToDetails(flat.flatPlatform, flat.adId)
                        },
                        clickOnFavorite = { clickOnFavorite(flat) },
                        onClose = { onFlatSelected(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlatBottomSheetContent(
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit,
    onClose: () -> Unit
) {
    // Вариация FlatCard для BottomSheet: компактная высота изображения, более плотные отступы
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        // Хэндл и строка действий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Закрыть")
            }
        }

        // Кликабельность всего шита на детали (кроме кнопок) — сделаем кликабельными блоки ниже
        // Блок изображений
        if (flat.imageUrls.isNotEmpty()) {
            BottomSheetImagePager(
                imageUrls = flat.imageUrls,
                savedInFavorite = flat.savedInFavorite,
                clickOnFavorite = clickOnFavorite
            )
        } else {
            BottomSheetEmptyImage()
        }

        Spacer(Modifier.height(8.dp))

        // Цена
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${flat.priceUsd.price} ${flat.priceUsd.currency}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            flat.priceByn.price?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${flat.priceByn.price} ${flat.priceByn.currency})",
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

        // Комнаты
        Text(
            text = "${flat.numberOfRooms}-комн.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Метро
        if (flat.metroStation.isNotBlank()) {
            Text(
                text = flat.metroStation,
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

@Composable
private fun BottomSheetEmptyImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomSheetImagePager(
    imageUrls: List<String>,
    savedInFavorite: Boolean,
    clickOnFavorite: () -> Unit
) {
    val pagerState = rememberPagerState { imageUrls.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Чуть выше, чем в листе, чтобы в шите комфортней
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AppAsyncImage(
                imageUrl = imageUrls[page],
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Избранное
        IconButton(
            onClick = { clickOnFavorite() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (savedInFavorite) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
        ) {
            Icon(
                imageVector = if (savedInFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Добавить в избранное",
                tint = if (savedInFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }

        // Пейджер индикатор
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(imageUrls.size) { index ->
                val color = if (index == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}


