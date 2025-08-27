package io.flatzen.screens.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.kmpapp.screens.ShimmerBox
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

    LaunchedEffect(Unit) {
        viewModel.onIntent(FlatListScreenAction.ScreenVisible)
        // Track screen view through MviAction
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
                state.isLoading && state.isLoadingMore.not() -> LoadingContent()
                state.flatList.isEmpty() -> EmptyScreenContent()
                else -> FlatGrid(
                    isLoadingMore = state.isLoadingMore,
                    noFlatsToLoadMore = state.noFlatsToLoadMore,
                    flats = state.flatList,
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
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatGrid(
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean,
    noFlatsToLoadMore: Boolean,
    flats: List<UiFlat>,
    onFlatClick: (UiFlat) -> Unit,
    clickOnFavorite: (UiFlat) -> Unit,
    onLoadMore: (Int) -> Unit
) {
    val lazyGridState: LazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val firstVisibleItemIndex by remember {
        derivedStateOf { lazyGridState.firstVisibleItemIndex }
    }

    val showScrollToTopBtn by remember {
        derivedStateOf { firstVisibleItemIndex >= 2 } // Появляется после прокрутки двух строк
    }

    LaunchedEffect(flats) {
        snapshotFlow {
            lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex == flats.lastIndex) {
                    onLoadMore(lastVisibleIndex)
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(flats, key = { it.adId }) { flat ->
                FlatCard(
                    flat = flat,
                    onClick = { onFlatClick(flat) },
                    clickOnFavorite = {
                        clickOnFavorite(flat)
                    }
                )
            }

            if (isLoadingMore) {
                items(2, key = { "loading_$it" }) {
                    SkeletonFlatCard()
                }
            }
        }

        if (noFlatsToLoadMore) {
            Text(
                text = "Квартиры с текущими фильтрами закончились",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(horizontal = 56.dp + 6.dp),
                textAlign = TextAlign.Center
            )
        }

        AnimatedVisibility(
            visible = false/*showScrollToTopBtn*/,
            modifier = Modifier
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        if (firstVisibleItemIndex < 8) {
                            lazyGridState.animateScrollToItem(0)
                        } else {
                            lazyGridState.scrollToItem(8)
                            lazyGridState.animateScrollToItem(0)
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
                    .height(100.dp),
                imageUrls = flat.imageUrls,
                savedInFavorite = flat.savedInFavorite,
                clickOnFavorite = clickOnFavorite
            )

            Spacer(Modifier.height(8.dp))

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
