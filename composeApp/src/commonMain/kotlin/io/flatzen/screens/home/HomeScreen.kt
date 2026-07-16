package io.flatzen.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.detail_agent
import flatzen.composeapp.generated.resources.detail_owner
import flatzen.composeapp.generated.resources.filter_deal_type
import flatzen.composeapp.generated.resources.filter_rent
import flatzen.composeapp.generated.resources.filter_sale
import flatzen.composeapp.generated.resources.home_ad_label
import flatzen.composeapp.generated.resources.list_commercial_rooms_suffix
import flatzen.composeapp.generated.resources.list_load_more
import flatzen.composeapp.generated.resources.list_no_more_flats
import flatzen.composeapp.generated.resources.list_page
import flatzen.composeapp.generated.resources.list_rooms_suffix
import flatzen.composeapp.generated.resources.no_data_available
import flatzen.composeapp.generated.resources.reset
import flatzen.composeapp.generated.resources.sort_cheapest
import flatzen.composeapp.generated.resources.sort_expensive
import flatzen.composeapp.generated.resources.sort_newest
import io.flatzen.ads.MAX_NATIVE_ADS_PER_BATCH
import io.flatzen.ads.NativeAdSlot
import io.flatzen.ads.NativeAdSlotStyle
import io.flatzen.ads.clearNativeAdReuseCache
import io.flatzen.animations.rememberShimmerProgress
import io.flatzen.common.localization.localizedArea
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.di.container
import io.flatzen.entities.SingleChoiceEntity
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.kmpapp.screens.ShimmerBox
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.FeedItem
import io.flatzen.monetization.ads.buildFeedItems
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.screens.map.FlatItemContent
import io.flatzen.themes.FlatHubTheme
import io.flatzen.uiExtensions.removeParentPadding
import io.flatzen.uiExtensions.thenIf
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterState
import io.flatzen.viewmodel.list.FlatListAction
import io.flatzen.viewmodel.list.FlatListIntent
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.PremiumUpsellInlineText
import io.flatzen.widgets.RentSaleButtons
import io.flatzen.widgets.SortBottomSheet
import io.flatzen.widgets.dialogs.ForceUpdateDialog
import io.flatzen.widgets.dialogs.SearchErrorDialog
import io.flatzen.widgets.dialogs.SingleChoiceDialog
import io.flatzen.widgets.rememberPremiumUpsellState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import io.flatzen.common.localization.stringResource as localizedStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val flatSearchContainer: FlatSearchContainer = koinInject()
    val lazyListState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val state by flatSearchContainer.store.subscribe { action ->
        when (action) {
            is FlatListAction.ScrollToTopEffect -> {
                coroutineScope.launch { lazyListState.scrollToItem(0) }
            }
        }
    }

    val filterContainer: FilterContainer = container()
    val filterScreenState by filterContainer.store.subscribe { }
    val filters = filterScreenState.filters
    val updateFilters: (FilterState) -> Unit = { newFilters ->
        filterContainer.intent(FilterScreenAction.UpdateFilter(newFilters, doNetworkCall = true))
    }

    val localDensity = LocalDensity.current
    var topAppBarSize by remember { mutableStateOf(DpSize.Zero) }
    val topAppBarHeight: Dp = remember(topAppBarSize) {
        topAppBarSize.height
    }
    var noFlatsBoxHeight by remember { mutableStateOf(0.dp) }

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showCommercialDialog by rememberSaveable { mutableStateOf(false) }
    val filterSummaryStrings = filterSummaryStrings()
    val premiumUpsell = rememberPremiumUpsellState(
        navigateToPremium = { flatSearchContainer.store.intent(FlatListIntent.OpenPremium) },
    )

    val firstVisibleItemIndex by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }

    val showScrollToTopBtn by remember {
        val count = if (state.isListView) 6 else 8
        derivedStateOf { firstVisibleItemIndex >= count && state.flatList.isNotEmpty() }
    }
    val scrollToTopBtnSize: Dp = 48.dp

    LaunchedEffect(Unit) {
        flatSearchContainer.store.intent(FlatListIntent.ScreenVisible)
    }

    LaunchedEffectOnce(Unit) {
        flatSearchContainer.store.intent(
            FlatListIntent.TrackScreenView(
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
                onClick = { flatSearchContainer.store.intent(FlatListIntent.OpenFilter) },
                isAnyFilterApplied = state.isAnyFilterApplied
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    flatSearchContainer.store.intent(
                        FlatListIntent.SearchFlats(
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
                        flatSearchContainer.store.intent(FlatListIntent.HideNetworkErrorDialog)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
                    .thenIf(premiumUpsell != null) {
                        background(FlatHubTheme.semantic.premiumDelayHint)
                    }
                    .onSizeChanged { size ->
                        topAppBarSize = localDensity.run {
                            DpSize(
                                size.width.toDp(),
                                size.height.toDp()
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (premiumUpsell != null) {
                    PremiumUpsellInlineText(
                        state = premiumUpsell,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                AsyncImage(
                    model = Res.getUri("drawable/outline_notifications.svg"),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            flatSearchContainer.store.intent(FlatListIntent.OpenNotifications)
                        },
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Spacer(Modifier.width(6.dp))

                TextButton(
                    onClick = {
                        updateFilters(FilterState())
                    }
                ) {
                    Text(stringResource(Res.string.reset))
                }
            }

            when {
                state.isLoading && state.isLoadingMore.not() -> LoadingContent(
                    modifier = Modifier.padding(top = topAppBarHeight),
                    filterState = filters,
                    isListView = state.isListView,
                    onToggleView = {
                        flatSearchContainer.store.intent(FlatListIntent.ToggleView)
                    }
                )

                state.isLoading.not() && state.flatList.isEmpty() -> {
                    LazyColumn(
                        modifier = modifier.fillMaxSize().padding(top = topAppBarHeight),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        topContentHeader(
                            isListView = state.isListView,
                            filterState = filters,
                            updateFilters = updateFilters,
                            onToggleView = {
                                flatSearchContainer.store.intent(FlatListIntent.ToggleView)
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
                                text = filters.getActiveFiltersText { key ->
                                    filterSummaryStrings[key] ?: key.name
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LoadMoreForce(state.currentSearchPage) {
                                clickOnLoadMore(flatSearchContainer)
                            }
                        }
                    }
                }

                else -> {
                    FlatList(
                        modifier = Modifier.padding(top = topAppBarHeight),
                        lazyListState = lazyListState,
                        isLoadingMore = state.isLoadingMore,
                        flats = state.flatList,
                        isListView = state.isListView,
                        onFlatClick = {
                            flatSearchContainer.store.intent(
                                FlatListIntent.OpenDetail(it.flatPlatform, it.adId)
                            )
                        },
                        clickOnFavorite = {
                            flatSearchContainer.store.intent(
                                FlatListIntent.ClickOnFavorite(
                                    it.flatPlatform,
                                    it.adId
                                )
                            )
                        },
                        clickOnClearDislike = {
                            flatSearchContainer.store.intent(
                                FlatListIntent.ClearDislike(
                                    it.flatPlatform,
                                    it.adId
                                )
                            )
                        },
                        onLoadMore = {
                            flatSearchContainer.store.intent(FlatListIntent.SearchFlats(true))
                        },
                        topContent = {
                            topContentHeader(
                                isListView = state.isListView,
                                filterState = filters,
                                updateFilters = updateFilters,
                                onToggleView = {
                                    flatSearchContainer.store.intent(FlatListIntent.ToggleView)
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
                                    LoadMoreForce(state.currentSearchPage) {
                                        clickOnLoadMore(flatSearchContainer)
                                    }
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

            ScrollToTopBtn(
                showScrollToTopBtn,
                firstVisibleItemIndex,
                lazyListState,
                scrollToTopBtnSize
            )

            if (state.noFlatsToLoadMore) {
                NoFlatsToLoadMoreText(
                    showScrollToTopBtn = showScrollToTopBtn,
                    onSizeChanged = {
                        noFlatsBoxHeight = with(localDensity) {
                            it.height.toDp()
                        }
                    })
            }

            if (showSortSheet) {
                SortBottomSheet(
                    selectedSortOption = filters.sortOption,
                    onOptionSelected = { sortOption: FlatSort ->
                        updateFilters(filters.copy(sortOption = sortOption))
                    },
                    onDismiss = { showSortSheet = false }
                )
            }

            if (showCommercialDialog) {
                SingleChoiceDialog(
                    title = stringResource(Res.string.filter_deal_type),
                    items = listOf(
                        SingleChoiceEntity(
                            title = stringResource(Res.string.filter_sale), AdType.COMMERCIAL(
                                CommercialAdType.SALE
                            )
                        ),
                        SingleChoiceEntity(
                            title = stringResource(Res.string.filter_rent), AdType.COMMERCIAL(
                                CommercialAdType.RENT
                            )
                        )
                    ),
                    selectedItem = filters.lastCommercialAdType,
                    onDismissRequest = {
                        showCommercialDialog = false
                    },
                    onSelected = { adType ->
                        updateFilters(
                            filters.copy(
                                adType = adType,
                                lastCommercialAdType = adType
                            )
                        )
                    }
                )
            }

        }
    }
}

@Composable
fun BoxScope.ScrollToTopBtn(
    showScrollToTopBtn: Boolean,
    firstVisibleItemIndex: Int,
    lazyListState: LazyListState,
    scrollToTopBtnSize: Dp
) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = showScrollToTopBtn,
        modifier = Modifier
            .padding(bottom = 16.dp)
            .align(Alignment.BottomCenter)
    ) {
        IconButton(
            onClick = {
                scope.launch {
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
                contentDescription = localizedStringResource(LocalizationKeys.SCROLL_TO_TOP),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun BoxScope.NoFlatsToLoadMoreText(
    showScrollToTopBtn: Boolean,
    onSizeChanged: (IntSize) -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(16.dp)
            .padding(horizontal = 56.dp + 6.dp)
            .thenIf(showScrollToTopBtn) {
                padding(bottom = 72.dp)
            }
            .background(FlatHubTheme.semantic.upsellBanner, FlatHubTheme.shapes.large)
            .padding(vertical = 4.dp)
            .onSizeChanged { size ->
                onSizeChanged(size)
            }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 2.dp),
            text = stringResource(Res.string.list_no_more_flats),
            color = FlatHubTheme.semantic.onUpsellBanner,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun clickOnLoadMore(container: io.flatzen.viewmodel.list.FlatSearchContainer) {
    container.store.intent(
        FlatListIntent.SearchFlats(
            isLoadMore = true,
            isLoadMoreForce = true
        )
    )
}

@Composable
fun LoadMoreForce(
    currentSearchPage: Int,
    loadMoreClick: () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(Res.string.list_page, currentSearchPage.toString()))
        TextButton(
            modifier = Modifier
                .wrapContentHeight()
                .padding(6.dp),
            onClick = {
                loadMoreClick()
            }) {
            Text(stringResource(Res.string.list_load_more))
        }
    }
}

@Composable
private fun LoadingContent(
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
        flatListSkeletons(isListView)
    }
}

fun LazyListScope.flatListSkeletons(isListView: Boolean) {
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

private object GridFlatItemSpec {
    val imageHeight = 100.dp
    val padding = 8.dp
    val spacerAfterImage = 8.dp
    val priceLineHeight = 20.dp
    val smallSpacer = 4.dp
    val roomsLineHeight = 18.dp
    val textLineHeight = 16.dp

    val skeletonHeight: Dp
        get() = padding * 2 +
                imageHeight +
                spacerAfterImage +
                priceLineHeight + smallSpacer +
                roomsLineHeight + smallSpacer +
                textLineHeight + smallSpacer +
                textLineHeight + smallSpacer +
                textLineHeight
}

@Composable
private fun SkeletonFlatGridItem(
    modifier: Modifier = Modifier
) {
    val shimmerProgress by rememberShimmerProgress()
    val spec = GridFlatItemSpec

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spec.padding)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spec.imageHeight),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(spec.spacerAfterImage))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(spec.priceLineHeight),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(spec.smallSpacer))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(spec.roomsLineHeight),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(spec.smallSpacer))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(spec.textLineHeight),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(spec.smallSpacer))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spec.textLineHeight),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(spec.smallSpacer))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(spec.textLineHeight),
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
            ListTypeSwitches(onToggleView, isListView, unSelectedColor, activeColor)

            filterState?.let {
                Spacer(Modifier.width(12.dp))
                AssistChip(
                    modifier = Modifier.wrapContentSize(),
                    onClick = showSortSheet,
                    label = { Text(text = filterState.sortOption.localizedText()) },
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

@Composable
fun ListTypeSwitches(
    onToggleView: () -> Unit,
    isListView: Boolean,
    unSelectedColor: Color,
    activeColor: Color
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

@Composable
private fun FlatSort.localizedText(): String {
    return when (this) {
        FlatSort.NEWEST_FIRST -> stringResource(Res.string.sort_newest)
        FlatSort.CHEAPEST_FIRST -> stringResource(Res.string.sort_cheapest)
        FlatSort.MOST_EXPENSIVE_FIRST -> stringResource(Res.string.sort_expensive)
    }
}

@Composable
private fun filterSummaryStrings(): Map<LocalizationKeys, String> {
    return mapOf(
        LocalizationKeys.FILTER_RENT to localizedStringResource(LocalizationKeys.FILTER_RENT),
        LocalizationKeys.FILTER_SALE to localizedStringResource(LocalizationKeys.FILTER_SALE),
        LocalizationKeys.FILTER_COMMERCIAL_SALE to localizedStringResource(LocalizationKeys.FILTER_COMMERCIAL_SALE),
        LocalizationKeys.FILTER_COMMERCIAL_RENT to localizedStringResource(LocalizationKeys.FILTER_COMMERCIAL_RENT),
        LocalizationKeys.FILTER_COMMERCIAL to localizedStringResource(LocalizationKeys.FILTER_COMMERCIAL),
        LocalizationKeys.FILTER_DAILY to localizedStringResource(LocalizationKeys.FILTER_DAILY),
        LocalizationKeys.FILTER_PROPERTY_TYPE to localizedStringResource(LocalizationKeys.FILTER_PROPERTY_TYPE),
        LocalizationKeys.FILTER_PRICE_LABEL to localizedStringResource(LocalizationKeys.FILTER_PRICE_LABEL),
        LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL to localizedStringResource(LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL),
        LocalizationKeys.DETAIL_TOTAL_AREA to localizedStringResource(LocalizationKeys.DETAIL_TOTAL_AREA),
        LocalizationKeys.DETAIL_ROOMS_COUNT to localizedStringResource(LocalizationKeys.DETAIL_ROOMS_COUNT),
        LocalizationKeys.FILTER_METRO_PREFIX to localizedStringResource(LocalizationKeys.FILTER_METRO_PREFIX),
        LocalizationKeys.FILTER_ADDRESS_PREFIX to localizedStringResource(LocalizationKeys.FILTER_ADDRESS_PREFIX),
        LocalizationKeys.FILTER_CITY_PREFIX to localizedStringResource(LocalizationKeys.FILTER_CITY_PREFIX),
        LocalizationKeys.FILTER_ACTIVE_AREAS_PREFIX to localizedStringResource(LocalizationKeys.FILTER_ACTIVE_AREAS_PREFIX),
        LocalizationKeys.FILTER_DISTRICTS_PREFIX to localizedStringResource(LocalizationKeys.FILTER_DISTRICTS_PREFIX),
        LocalizationKeys.FILTER_OWNER_ONLY to localizedStringResource(LocalizationKeys.FILTER_OWNER_ONLY),
        LocalizationKeys.FILTER_PHOTO_ONLY to localizedStringResource(LocalizationKeys.FILTER_PHOTO_ONLY),
        LocalizationKeys.FILTER_ROOM_ONLY to localizedStringResource(LocalizationKeys.FILTER_ROOM_ONLY),
        LocalizationKeys.FILTER_ACTIVE_NONE to localizedStringResource(LocalizationKeys.FILTER_ACTIVE_NONE),
        LocalizationKeys.FILTER_ACTIVE_TITLE to localizedStringResource(LocalizationKeys.FILTER_ACTIVE_TITLE),
        LocalizationKeys.FILTER_TYPE_PREFIX to localizedStringResource(LocalizationKeys.FILTER_TYPE_PREFIX),
        LocalizationKeys.FROM to localizedStringResource(LocalizationKeys.FROM),
        LocalizationKeys.TO to localizedStringResource(LocalizationKeys.TO),
        LocalizationKeys.COMMERCIAL_PROPERTY_ALL to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_ALL),
        LocalizationKeys.COMMERCIAL_PROPERTY_INDUSTRIAL to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_INDUSTRIAL),
        LocalizationKeys.COMMERCIAL_PROPERTY_OFFICE to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_OFFICE),
        LocalizationKeys.COMMERCIAL_PROPERTY_RETAIL to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_RETAIL),
        LocalizationKeys.COMMERCIAL_PROPERTY_SERVICES to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_SERVICES),
        LocalizationKeys.COMMERCIAL_PROPERTY_WAREHOUSES to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_WAREHOUSES),
        LocalizationKeys.COMMERCIAL_PROPERTY_OTHER to localizedStringResource(LocalizationKeys.COMMERCIAL_PROPERTY_OTHER),
    )
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
    clickOnClearDislike: (UiFlat) -> Unit = {},
    onLoadMore: (Int) -> Unit,
    topContent: LazyListScope.() -> Unit = {},
    bottomContent: LazyListScope.() -> Unit = {}
) {
    val userTierProvider: UserTierProvider = koinInject()
    val monetizationConfig: MonetizationRemoteConfig = koinInject()
    val adService: AdService = koinInject()
    val adInterval = if (isListView == true) {
        monetizationConfig.homeListAdInterval
    } else {
        monetizationConfig.homeGridAdInterval
    }
    val feedItems =
        remember(flats, adInterval, userTierProvider.shouldShowAds()) {
            buildFeedItems(
                flats = flats,
                interval = adInterval,
                showAds = userTierProvider.shouldShowAds(),
            )
        }
    val gridRows = remember(feedItems) { buildGridRows(feedItems) }
    val homeFeedPlacement = if (isListView == true) {
        monetizationConfig.homeFeedListPlacement
    } else {
        monetizationConfig.homeFeedGridPlacement
    }

    DisposableEffect(Unit) {
        onDispose { clearNativeAdReuseCache() }
    }

    LaunchedEffect(feedItems, homeFeedPlacement, adService.isInitialized()) {
        if (!adService.isInitialized() || homeFeedPlacement.isBlank()) return@LaunchedEffect
        val adSlots = feedItems.count { it is FeedItem.Ad }
            .coerceIn(0, MAX_NATIVE_ADS_PER_BATCH)
        if (adSlots > 0) {
            adService.prefetchNative(homeFeedPlacement, adSlots)
        }
    }

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
            items(
                count = feedItems.size,
                key = { index ->
                    when (val item = feedItems[index]) {
                        is FeedItem.Flat -> "flat-${item.value.adId}"
                        FeedItem.Ad -> "ad-$index"
                    }
                }
            ) { index ->
                when (val item = feedItems[index]) {
                    is FeedItem.Flat -> {
                        val flat = item.value
                        ListFlatCard(
                            flat = flat,
                            onClick = { onFlatClick(flat) },
                            clickOnFavorite = { clickOnFavorite(flat) },
                            clickOnClearDislike = { clickOnClearDislike(flat) },
                        )
                    }

                    FeedItem.Ad -> {
                        HomeFeedAdBlock(
                            placement = monetizationConfig.homeFeedGridPlacement,
                            reuseKey = "home-list-$index",
                            style = NativeAdSlotStyle.AppWall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = GridFlatItemSpec.skeletonHeight),
                        )
                    }
                }
            }
        } else {
            // Grid: Ad spans full width
            items(
                count = gridRows.size,
                key = { index ->
                    when (val row = gridRows[index]) {
                        is GridRow.Pair -> "row-${row.first.adId}"
                        is GridRow.Ad -> "ad-row-$index"
                    }
                }
            ) { index ->
                when (val row = gridRows[index]) {
                    is GridRow.Pair -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GridFlatCard(
                                flat = row.first,
                                onClick = { onFlatClick(row.first) },
                                clickOnFavorite = { clickOnFavorite(row.first) },
                                clickOnClearDislike = { clickOnClearDislike(row.first) },
                                modifier = Modifier.weight(1f)
                            )
                            row.second?.let { second ->
                                GridFlatCard(
                                    flat = second,
                                    onClick = { onFlatClick(second) },
                                    clickOnFavorite = { clickOnFavorite(second) },
                                    clickOnClearDislike = { clickOnClearDislike(second) },
                                    modifier = Modifier.weight(1f)
                                )
                            } ?: Spacer(Modifier.weight(1f))
                        }
                    }

                    is GridRow.Ad -> HomeFeedAdBlock(
                        placement = monetizationConfig.homeFeedGridPlacement,
                        reuseKey = "home-grid-$index",
                        style = NativeAdSlotStyle.AppWall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = GridFlatItemSpec.skeletonHeight),
                    )
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

private sealed class GridRow {
    data class Pair(val first: UiFlat, val second: UiFlat?) : GridRow()
    data object Ad : GridRow()
}

@Composable
private fun HomeFeedAdBlock(
    placement: String,
    reuseKey: String,
    style: NativeAdSlotStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Native (not MREC): Appodeal MREC is a single shared view and breaks in LazyColumn recycle.
        NativeAdSlot(
            placement = placement,
            reuseKey = reuseKey,
            style = style,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        Text(
            text = stringResource(Res.string.home_ad_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

private fun buildGridRows(feedItems: List<FeedItem<UiFlat>>): List<GridRow> {
    val rows = mutableListOf<GridRow>()
    var pending: UiFlat? = null
    feedItems.forEach { item ->
        when (item) {
            is FeedItem.Ad -> {
                pending?.let {
                    rows += GridRow.Pair(it, null)
                    pending = null
                }
                rows += GridRow.Ad
            }

            is FeedItem.Flat -> {
                val flat = item.value
                if (pending == null) {
                    pending = flat
                } else {
                    rows += GridRow.Pair(pending!!, flat)
                    pending = null
                }
            }
        }
    }
    pending?.let { rows += GridRow.Pair(it, null) }
    return rows
}

@Composable
private fun ListFlatCard(
    modifier: Modifier = Modifier,
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit,
    clickOnClearDislike: () -> Unit = {},
) {
    val dimens = FlatHubTheme.dimens
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FlatHubTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationCard),
        border = BorderStroke(dimens.cardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        FlatItemContent(
            flat = flat,
            onClick = onClick,
            clickOnFavorite = clickOnFavorite,
            clickOnClearDislike = clickOnClearDislike,
        )
    }
}

@Composable
private fun GridFlatCard(
    modifier: Modifier = Modifier,
    flat: UiFlat,
    onClick: () -> Unit,
    clickOnFavorite: () -> Unit,
    clickOnClearDislike: () -> Unit = {},
) {
    val dimens = FlatHubTheme.dimens
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = FlatHubTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationCard),
        border = BorderStroke(dimens.cardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.cardPaddingCompact)
        ) {
            FlatImagePager(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GridFlatItemSpec.imageHeight)
                    .removeParentPadding(dimens.cardPaddingCompact),
                flatPlatform = flat.flatPlatform,
                imageUrls = flat.imageUrls,
                isViewed = flat.isViewed,
                savedInFavorite = flat.savedInFavorite,
                saveInFavoriteInProgress = flat.saveInFavoriteInProgress,
                disliked = flat.disliked,
                clickOnFavorite = clickOnFavorite,
                clickOnClearDislike = clickOnClearDislike,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = secondPriceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            flat.publishedAt?.let { date ->
                Spacer(Modifier.height(4.dp))

                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Keep slot even when platform (e.g. Onliner) does not provide owner/agent.
            Spacer(modifier.height(4.dp))
            Text(
                text = when (flat.isOwner) {
                    true -> stringResource(Res.string.detail_owner)
                    false -> stringResource(Res.string.detail_agent)
                    null -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
            )
            val propertyTypeName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
            val propertyTypeRoom = flat.commercialUiInfo?.numberOfRooms
            propertyTypeName?.let { name ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localizedStringResource(name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(4.dp))

            val roomSuffix = if (propertyTypeRoom.isNullOrEmpty().not()) {
                stringResource(Res.string.list_commercial_rooms_suffix)
            } else {
                stringResource(Res.string.list_rooms_suffix)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${flat.numberOfRooms} $roomSuffix,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (flat.totalArea.isNullOrEmpty().not()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = localizedArea(flat.totalArea!!),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = flat.metroStation.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = flat.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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