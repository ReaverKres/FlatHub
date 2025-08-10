package io.flatzen.screens.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.kmpapp.screens.ShimmerBox
import io.flatzen.viewmodel.FlatListScreenAction
import io.flatzen.viewmodel.FlatSearchViewModel
import io.flatzen.viewmodel.UiFlat
import io.flatzen.widgets.AppAsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

@Composable
fun ListScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateToFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<FlatSearchViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = navigateToFilters) {
                Icon(Icons.Default.Build, contentDescription = "Фильтры")
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.isLoadingMore.not() -> LoadingContent()
            state.flatList.isEmpty() -> EmptyScreenContent()
            else -> FlatGrid(
                isLoadingMore = state.isLoadingMore,
                noFlatsToLoadMore = state.noFlatsToLoadMore,
                flats = state.flatList,
                onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
                clickOnFavorite = {
                    viewModel.onIntent(FlatListScreenAction.ClickOnFavorite(it.flatPlatform, it.adId))
                },
                onLoadMore = {
                    viewModel.onIntent(FlatListScreenAction.SearchFlats(true))
                }
            )
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
                    .fillMaxWidth(0.6f)
                    .height(20.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(4.dp))

            // Скелетон цены BYN
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp),
                shimmerProgress = shimmerProgress
            )

            Spacer(Modifier.height(8.dp))

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
    LaunchedEffect(flats) {
        snapshotFlow {
            lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if(lastVisibleIndex == flats.lastIndex) {
                    onLoadMore(lastVisibleIndex)
                }
            }
    }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(180.dp),
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
                    .padding(horizontal = 56.dp + 6.dp)
                ,
                textAlign = TextAlign.Center
            )
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
            if (flat.imageUrls.isNotEmpty()) {
                ImagePager(flat.imageUrls, flat.savedInFavorite,  clickOnFavorite)
            } else {
                FlatEmptyImage()
            }

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

@Composable
private fun FlatEmptyImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ImagePager(
    imageUrls: List<String>,
    savedInFavorite: Boolean = false,
    clickOnFavorite: () -> Unit = {},
    ) {
    val pagerState = rememberPagerState { imageUrls.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
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

        IconButton(
            onClick = { clickOnFavorite() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(24.dp)
        ) {
            Icon(
                imageVector = if (savedInFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Добавить в избранное",
                tint = if (savedInFavorite) Color.Red else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxSize()
            )
        }

        // Индикатор страниц
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(imageUrls.size) { index ->
                val color = if (index == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
