package io.flatzen.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.no_data_available
import io.flatzen.ForceUpdateDialog
import io.flatzen.SearchErrorDialog
import io.flatzen.SingleChoiceDialog
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.utils.formatMainPrice
import io.flatzen.commoncomponents.utils.formatSecondPrice
import io.flatzen.entities.SingleChoiceEntity
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.kmpapp.screens.ShimmerBox
import io.flatzen.screens.map.FlatItemContent
import io.flatzen.uiExtensions.removeParentPadding
import io.flatzen.uiExtensions.thenIf
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.utils.text
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterState
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatListEffect
import io.flatzen.viewmodel.list.FlatListScreenAction
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.RentSaleButtons
import io.flatzen.widgets.SortBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateToFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<FlatSearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filterViewModel = koinViewModel<FilterViewModel>()
    val filterScreenState by filterViewModel.state.collectAsStateWithLifecycle()
    var currentFilters by remember(filterScreenState.filters) { mutableStateOf(filterScreenState.filters) }

    val localDensity = LocalDensity.current
    var resetFilterButtonSize by remember { mutableStateOf(DpSize.Zero) }
    val resetFilterButtonHeight: Dp = remember(resetFilterButtonSize) {
        resetFilterButtonSize.height
    }
    var noFlatsBoxHeight by remember { mutableStateOf(0.dp) }

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showCommercialDialog by rememberSaveable { mutableStateOf(false) }

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
        derivedStateOf { firstVisibleItemIndex >= count && state.flatList.isNotEmpty() }
    }
    val scrollToTopBtnSize: Dp = 48.dp

    LaunchedEffect(Unit) {
        viewModel.onIntent(FlatListScreenAction.ScreenVisible)
        viewModel.effect.collect {
            when (it) {
                is FlatListEffect.ScrollToTop -> {
                    lazyListState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffectOnce(Unit) {
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
            if (state.infoDialogState?.isVisible == true && state.infoDialogState?.dialogType == DialogType.ForceUpdate) {
                ForceUpdateDialog(state.infoDialogState!!)
            }

            if (state.errorDialogState?.isVisible == true) {
                SearchErrorDialog(
                    dialogState = state.errorDialogState!!,
                    onDismiss = {
                        viewModel.onIntent(FlatListScreenAction.HideNetworkErrorDialog)
                    }
                )
            }

            TextButton(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(6.dp)
                    .align(Alignment.TopEnd)
                    .onSizeChanged { size ->
                        resetFilterButtonSize = localDensity.run {
                            DpSize(
                                size.width.toDp(),
                                size.height.toDp()
                            )
                        }
                        resetFilterButtonSize
                    },
                onClick = {
                    currentFilters = FilterState()
                }) {
                Text("Сбросить фильтр")
            }

            when {
                state.isLoading && state.isLoadingMore.not() -> LoadingContent(
                    modifier = Modifier.padding(top = resetFilterButtonHeight),
                    filterState = currentFilters,
                    isListView = state.isListView,
                    onToggleView = {
                        viewModel.onIntent(FlatListScreenAction.ToggleView)
                    }
                )

                state.isLoading.not() && state.flatList.isEmpty() -> {
                    LazyColumn(
                        modifier = modifier.fillMaxSize().padding(top = resetFilterButtonHeight),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        topContentHeader(
                            isListView = state.isListView,
                            filterState = currentFilters,
                            updateFilters = {
                                currentFilters = it
                            },
                            onToggleView = {
                                viewModel.onIntent(FlatListScreenAction.ToggleView)
                            }
                        )
                        item {
                            Spacer(Modifier.height(32.dp))
                            EmptyScreenContent(
                                modifier = Modifier.fillMaxWidth(),
                                stringResource = Res.string.no_data_available
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = currentFilters.getActiveFiltersText(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LoadMoreForce(state.currentSearchPage, viewModel)
                        }
                    }
                }

                else -> {
                    FlatList(
                        modifier = Modifier.padding(top = resetFilterButtonHeight),
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
                                filterState = currentFilters,
                                updateFilters = {
                                    currentFilters = it
                                },
                                onToggleView = {
                                    viewModel.onIntent(FlatListScreenAction.ToggleView)
                                },
                                showSortSheet = {
                                    showSortSheet = true
                                },
                                showCommercialDialog = {
                                    showCommercialDialog = true
                                }
                            )
                        },
                        bottomContent = {
                            if (state.noFlatsToLoadMore) {
                                item {
                                    LoadMoreForce(state.currentSearchPage, viewModel)
                                    Spacer(Modifier.height(noFlatsBoxHeight))
                                    Spacer(Modifier.height(16.dp))
                                    if (showScrollToTopBtn) {
                                        Spacer(Modifier.height(scrollToTopBtnSize + 24.dp))
                                    }
                                }
                            }
                        }
                    )
                }
            }

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
                        .size(scrollToTopBtnSize)
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
                        .onSizeChanged { size ->
                            noFlatsBoxHeight = with(localDensity) {
                                size.height.toDp()
                            }
                        }
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 2.dp),
                        text = "Квартиры с текущими фильтрами закончились",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (showSortSheet) {
                SortBottomSheet(
                    selectedSortOption = currentFilters.sortOption,
                    onOptionSelected = { sortOption: FlatSort ->
                        currentFilters = currentFilters.copy(sortOption = sortOption)
                    },
                    onDismiss = { showSortSheet = false }
                )
            }

            if (showCommercialDialog) {
                SingleChoiceDialog(
                    title = "Тип сделки",
                    items = listOf(
                        SingleChoiceEntity(
                            title = "Продажа", AdType.COMMERCIAL(
                                CommercialAdType.SALE
                            )
                        ),
                        SingleChoiceEntity(
                            title = "Аренда", AdType.COMMERCIAL(
                                CommercialAdType.RENT
                            )
                        )
                    ),
                    selectedItem = currentFilters.lastCommercialAdType,
                    onDismissRequest = {
                        showCommercialDialog = false
                    },
                    onSelected = { adType ->
                        currentFilters = currentFilters.copy(
                            adType = adType,
                            lastCommercialAdType = adType
                        )
                    }
                )
            }

        }
    }
}

@Composable
private fun LoadMoreForce(
    currentSearchPage: Int,
    viewModel: FlatSearchViewModel
) {
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Страница: $currentSearchPage")
        TextButton(
            modifier = Modifier
                .wrapContentHeight()
                .padding(6.dp),
            onClick = {
                viewModel.onIntent(
                    FlatListScreenAction.SearchFlats(
                        isLoadMore = true,
                        isLoadMoreForce = true
                    )
                )
            }) {
            Text("Загрузить больше")
        }
    }
}

@Composable
fun LoadingContent(
    isListView: Boolean,
    filterState: FilterState,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        topContentHeader(
            isListView = isListView,
            filterState = filterState,
            updateFilters = {},
            onToggleView = onToggleView
        )
        if (isListView) {
            items(10) {
                SkeletonFlatListItem()
            }
        } else {
            items(10) { flatPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonFlatGridItem(modifier = Modifier.weight(1f))
                    SkeletonFlatGridItem(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SkeletonFlatGridItem(
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
    filterState: FilterState?,
    updateFilters: (FilterState) -> Unit,
    onToggleView: () -> Unit,
    showSortSheet: () -> Unit = {},
    showCommercialDialog: () -> Unit = {}
) {

    filterState?.let {
        item {
            RentSaleButtons(
                selectedAdType = filterState.adType,
                lastCommercialAdType = filterState.lastCommercialAdType,
                onClick = { adType ->
                    updateFilters(filterState.copy(adType = adType))
                },
                fewTypeInOneClick = { adType ->
                    if (adType.isCommercial) {
                        showCommercialDialog()
                    }
                }
            )
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
            verticalAlignment = Alignment.CenterVertically
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

            filterState?.let {
                Spacer(Modifier.width(12.dp))
                AssistChip(
                    modifier = Modifier.wrapContentSize(),
                    onClick = showSortSheet,
                    label = { Text(text = filterState.sortOption.text) },
                    trailingIcon = {
                        AsyncImage(
                            model = Res.getUri("drawable/sort.svg"),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(unSelectedColor)
                        )
                    }
                )
            }
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
    topContent: LazyListScope.() -> Unit = {},
    bottomContent: LazyListScope.() -> Unit = {}
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
                ListFlatCard(
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
                items = flats.chunked(2),
                key = { it.firstOrNull()?.adId ?: it.hashCode() }
            ) { flatPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // First item
                    flatPair.getOrNull(0)?.let { flat ->
                        GridFlatCard(
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
                        GridFlatCard(
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
                        SkeletonFlatGridItem(modifier = Modifier.weight(1f))
                        SkeletonFlatGridItem(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        bottomContent()
    }
}

@Composable
private fun ListFlatCard(
    modifier: Modifier = Modifier,
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        FlatItemContent(
            flat = flat,
            onClick = onClick,
            clickOnFavorite = clickOnFavorite
        )
    }
}

@Composable
private fun GridFlatCard(
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
                saveInFavoriteInProgress = flat.saveInFavoriteInProgress,
                clickOnFavorite = clickOnFavorite
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bynMainPrice = flat.priceUsd == null && flat.priceByn != null
                val mainPriceText = if (bynMainPrice) {
                    formatMainPrice(flat.priceByn, "BYN")
                } else if(flat.priceUsd != null) {
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
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = secondPriceText,
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
            val propertyTypeName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
            val propertyTypeRoom = flat.commercialUiInfo?.numberOfRooms
            if (propertyTypeName.isNullOrEmpty().not()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = propertyTypeName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(4.dp))

            val roomSuffix = if (propertyTypeRoom.isNullOrEmpty().not()) {
                "помещений"
            } else {
                "комн"
            }
            Text(
                text = "${flat.numberOfRooms} $roomSuffix",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = flat.metroStation.orEmpty(),
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