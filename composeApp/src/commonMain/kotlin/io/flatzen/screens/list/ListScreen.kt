package io.flatzen.screens.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.no_data_available
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.kmpapp.screens.ShimmerBox
import io.flatzen.screens.filter.RentSaleSegmentedButtons
import io.flatzen.screens.filter.SortOptionSegmentedButtons
import io.flatzen.screens.map.FlatItemContent
import io.flatzen.uiExtensions.removeParentPadding
import io.flatzen.uiExtensions.thenIf
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterState
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatListScreenAction
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatImagePager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateToFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<FlatSearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filterViewModel = koinViewModel<FilterViewModel>()
    val filterScreenState by filterViewModel.state.collectAsStateWithLifecycle()
    var currentFilters by remember(filterScreenState.filters) { mutableStateOf(filterScreenState.filters) }

    LaunchedEffect(currentFilters) {
        filterViewModel.onIntent(FilterScreenAction.UpdateFilter(currentFilters, true))
    }

    val lazyListState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val firstVisibleItemIndex by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }

    val showScrollToTopBtn by remember {
        val count = if (state.isListView) 6 else 8
        derivedStateOf { firstVisibleItemIndex >= count }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(FlatListScreenAction.ScreenVisible)
        viewModel.onIntent(
            FlatListScreenAction.TrackScreenView(
                screenName = AppMetrcica.Screens.LIST,
                parameters = mapOf(
                    AppMetrcica.Parameters.SCREEN_TYPE to AppMetrcica.ScreenTypes.MAIN
                )
            )
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FilterActionButton(
                onClick = navigateToFilters,
                isAnyFilterApplied = state.isAnyFilterApplied
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    viewModel.onIntent(
                        FlatListScreenAction.SearchFlats(
                            isLoadMore = false,
                            isRefreshing = true
                        )
                    )
                }
            },
            isRefreshing = state.isRefreshing
        ) {
            when {
                state.isLoading && state.isLoadingMore.not() -> LoadingContent(isListView = state.isListView)
                state.isLoading.not() && state.flatList.isEmpty() -> EmptyScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    stringResource = Res.string.no_data_available
                )

                else -> {
                    FlatList(
                        lazyListState = lazyListState,
                        isLoadingMore = state.isLoadingMore,
                        flats = state.flatList,
                        isListView = state.isListView,
                        onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
                        clickOnFavorite = {
                            viewModel.onIntent(
                                FlatListScreenAction.ClickOnFavorite(
                                    it.flatPlatform,
                                    it.adId
                                )
                            )
                        },
                        onLoadMore = {
                            viewModel.onIntent(FlatListScreenAction.SearchFlats(true))
                        },
                        topContent = {
                            topContentHeader(
                                isListView = state.isListView,
                                filterState = filterScreenState.filters,
                                updateFilters = {
                                    currentFilters = it
                                },
                                onToggleView = {
                                    viewModel.onIntent(FlatListScreenAction.ToggleView)
                                }
                            )
                        }
                    )
                }
            }

            // Кнопка прокрутки наверх - ВНЕ PullToRefreshBox
            AnimatedVisibility(
                visible = showScrollToTopBtn,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (firstVisibleItemIndex < 8) {
                                lazyListState.animateScrollToItem(0)
                            } else {
                                lazyListState.scrollToItem(8)
                                lazyListState.animateScrollToItem(0)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Сообщение о конце списка - ВНЕ PullToRefreshBox
            if (state.noFlatsToLoadMore) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(horizontal = 56.dp + 6.dp)
                        .thenIf(showScrollToTopBtn) {
                            padding(bottom = 72.dp)
                        }
                        .background(Color(0xFFbf4f1f), RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Квартиры с текущими фильтрами закончились",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

            }

        }
    }
}

@Composable
fun LoadingContent(
    isListView: Boolean,
    modifier: Modifier = Modifier
) {
    if (isListView) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(10) {
                SkeletonFlatListItem()
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(10) {
                SkeletonFlatCard()
            }
        }
    }
}

@Composable
private fun SkeletonFlatCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Скелетон изображения
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(8.dp))

            // Скелетон цены USD
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(4.dp))

            // Скелетон количества комнат
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(18.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(4.dp))

            // Скелетон метро
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(4.dp))

            // Скелетон адреса (2 строки)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(4.dp))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp),
                shimmerProgress = shimmerProgress
            )
        }
    }
}

private fun LazyListScope.topContentHeader(
    isListView: Boolean,
    filterState: FilterState,
    updateFilters: (FilterState) -> Unit,
    onToggleView: () -> Unit,
) {

    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            RentSaleSegmentedButtons(filterState.adType) {
                updateFilters(filterState.copy(adType = it))
            }
        }
    }

    item {
        SortOptionSegmentedButtons(filterState.sortOption) { sortOption ->
            updateFilters(filterState.copy(sortOption = sortOption))
        }
    }

    item {
        val activeColor = MaterialTheme.colorScheme.primary
        val unSelectedColor = Color.LightGray

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
        ) {
            AsyncImage(
                model = Res.getUri("drawable/grid.svg"),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable {
                    onToggleView()
                },
                colorFilter = ColorFilter.tint(if (isListView) unSelectedColor else activeColor)
            )
            Spacer(Modifier.width(16.dp))
            AsyncImage(
                model = Res.getUri("drawable/list.svg"),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clickable {
                    onToggleView()
                },
                colorFilter = ColorFilter.tint(if (isListView) activeColor else unSelectedColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isLoadingMore: Boolean,
    flats: List<UiFlat>,
    isListView: Boolean? = null,
    onFlatClick: (UiFlat) -> Unit,
    clickOnFavorite: (UiFlat) -> Unit,
    onLoadMore: (Int) -> Unit,
    topContent: LazyListScope.() -> Unit = {}
) {
    LaunchedEffect(flats, isListView, isLoadingMore) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex == null || flats.isEmpty() || isLoadingMore) {
                    return@collect
                }
                val lastItemIndex = if (isListView == true) {
                    flats.size
                } else {
                    flats.chunked(2).size
                }
                if (lastVisibleIndex >= lastItemIndex - 3) {
                    onLoadMore(lastVisibleIndex)
                }
            }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        topContent()

        if (isListView == true) {
            // List view - one item per row
            items(flats, key = { it.adId }) { flat ->
                FlatItemContent(
                    flat = flat,
                    onClick = { onFlatClick(flat) },
                    clickOnFavorite = {
                        clickOnFavorite(flat)
                    }
                )
            }
        } else {
            // Grid view - two items per row
            items(
                flats.chunked(2),
                key = { it.firstOrNull()?.adId ?: it.hashCode() }) { flatPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // First item
                    flatPair.getOrNull(0)?.let { flat ->
                        FlatCard(
                            flat = flat,
                            onClick = { onFlatClick(flat) },
                            clickOnFavorite = {
                                clickOnFavorite(flat)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } ?: Spacer(Modifier.weight(1f))

                    // Second item
                    flatPair.getOrNull(1)?.let { flat ->
                        FlatCard(
                            flat = flat,
                            onClick = { onFlatClick(flat) },
                            clickOnFavorite = {
                                clickOnFavorite(flat)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } ?: Spacer(Modifier.weight(1f))
                }
            }
        }

        if (isLoadingMore) {
            item {
                if (isListView == true) {
                    SkeletonFlatListItem()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonFlatCard(modifier = Modifier.weight(1f))
                        SkeletonFlatCard(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatCard(
    modifier: Modifier = Modifier,
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            FlatImagePager(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .removeParentPadding(8.dp),
                flatPlatform = flat.flatPlatform,
                imageUrls = flat.imageUrls,
                isViewed = flat.isViewed,
                savedInFavorite = flat.savedInFavorite,
                clickOnFavorite = clickOnFavorite
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${flat.priceUsd.price} ${flat.priceUsd.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                flat.priceByn.price?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "(${flat.priceByn.price} ${flat.priceByn.currency})",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            flat.publishedAt?.let { date ->
                Spacer(Modifier.height(4.dp))

                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${flat.numberOfRooms}-комн.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = flat.metroStation,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = flat.address,
                style = MaterialTheme.typography.bodySmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SkeletonFlatListItem(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Скелетон изображения
            ShimmerBox(
                modifier = Modifier
                    .size(100.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                // Скелетон цены USD
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp),
                    shimmerProgress = shimmerProgress
                )

                Spacer(Modifier.height(4.dp))

                // Скелетон количества комнат
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(18.dp),
                    shimmerProgress = shimmerProgress
                )

                Spacer(Modifier.height(4.dp))

                // Скелетон метро
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp),
                    shimmerProgress = shimmerProgress
                )

                Spacer(Modifier.height(4.dp))

                // Скелетон адреса (2 строки)
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    shimmerProgress = shimmerProgress
                )
            }
        }
    }
}