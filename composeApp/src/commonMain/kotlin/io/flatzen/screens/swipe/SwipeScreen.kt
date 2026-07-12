package io.flatzen.screens.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.no_data_available
import io.flatzen.common.localization.localizedArea
import io.flatzen.common.localization.localizedRoomsLabel
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.di.container
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.themes.FlatHubTheme
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.list.FlatListIntent
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatZenFloatingActionButton
import io.flatzen.widgets.FlatZenOverlayChip
import io.flatzen.widgets.RememberPremiumUpsellBanner
import io.flatzen.widgets.SwipeableCard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.subscribe
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import kotlin.math.abs

private fun UiFlat.deckKey(): String = "${flatPlatform.name}:$adId"

private const val STACK_SCALE_STEP = 0.04f
private const val STACK_Y_STEP = 10f
private const val STACK_ALPHA_STEP = 0.08f

private fun stackCardTransform(depth: Int, promoteProgress: Float): StackCardTransform {
    val effectiveDepth = (depth - promoteProgress).coerceAtLeast(0f)
    return StackCardTransform(
        scale = 1f - effectiveDepth * STACK_SCALE_STEP,
        translationY = effectiveDepth * STACK_Y_STEP,
        alpha = 1f - effectiveDepth * STACK_ALPHA_STEP,
    )
}

private data class StackCardTransform(
    val scale: Float,
    val translationY: Float,
    val alpha: Float,
)

private enum class SwipeOutcome { Liked, Disliked }

private data class SwipeUndoEntry(
    val flatPlatform: FlatPlatform,
    val adId: Long,
    val outcome: SwipeOutcome,
) {
    fun deckKey(): String = "${flatPlatform.name}:$adId"
}

@Composable
fun SwipeScreen() {
    val flatSearchContainer: FlatSearchContainer = koinInject()
    val listState by flatSearchContainer.store.subscribe { }
    val filterContainer: FilterContainer = container()
    val filterScreenState by filterContainer.store.subscribe { }
    val filterRepository: FilterRepository = koinInject()
    val premiumBanner = RememberPremiumUpsellBanner(
        navigateToPremium = { flatSearchContainer.store.intent(FlatListIntent.OpenPremium) },
    )
    var swipeCount by remember { mutableIntStateOf(0) }
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var stackPromoteProgress by remember { mutableFloatStateOf(0f) }
    val userTierProvider: UserTierProvider = koinInject()
    val monetizationConfig: MonetizationRemoteConfig = koinInject()
    val adService: AdService = koinInject()
    val swipeScope = rememberCoroutineScope()

    // Optimistic dismiss so the next card appears before Room/MVI finishes.
    var pendingDismissKeys by remember { mutableStateOf(emptySet<String>()) }
    // Keep the same top card after detail open/return (even if isViewed was set earlier).
    var pinnedFrontKey by rememberSaveable { mutableStateOf<String?>(null) }
    var undoStack by remember { mutableStateOf(listOf<SwipeUndoEntry>()) }

    val isSearchLoading = listState.isLoading || listState.isRefreshing

    LaunchedEffect(Unit) {
        flatSearchContainer.store.intent(FlatListIntent.ScreenVisible)
        if (listState.flatList.isEmpty() && !listState.isLoading) {
            flatSearchContainer.store.intent(FlatListIntent.SearchFlats(isLoadMore = false))
        }
    }

    // New search (e.g. after filters) invalidates undo history.
    LaunchedEffect(isSearchLoading) {
        if (isSearchLoading && !listState.isLoadingMore) {
            undoStack = emptyList()
            pendingDismissKeys = emptySet()
            pinnedFrontKey = null
        }
    }

    // Drop pending keys once state has absorbed favorite/dislike.
    LaunchedEffect(listState.flatList) {
        if (pendingDismissKeys.isEmpty()) return@LaunchedEffect
        pendingDismissKeys = pendingDismissKeys.filter { key ->
            val flat = listState.flatList.find { it.deckKey() == key } ?: return@filter false
            !flat.savedInFavorite && !flat.isViewed && !flat.disliked
        }.toSet()
    }

    val deck by remember(listState.flatList, pendingDismissKeys, pinnedFrontKey) {
        derivedStateOf {
            val swipeDeck = listState.flatList.filter {
                !it.savedInFavorite &&
                        !it.isViewed &&
                        !it.disliked &&
                        it.deckKey() !in pendingDismissKeys
            }
            val pinned = pinnedFrontKey?.let { key ->
                listState.flatList.find {
                    it.deckKey() == key && it.deckKey() !in pendingDismissKeys
                }
            }
            when {
                pinned == null -> swipeDeck
                else -> listOf(pinned) + swipeDeck.filter { it.deckKey() != pinned.deckKey() }
            }
        }
    }

    LaunchedEffect(deck.size, listState.isLoadingMore, listState.noFlatsToLoadMore) {
        if (deck.size <= 3 && !listState.isLoadingMore && !listState.noFlatsToLoadMore && !listState.isLoading) {
            flatSearchContainer.store.intent(FlatListIntent.SearchFlats(isLoadMore = true))
        }
    }

    LaunchedEffect(
        deck.getOrNull(0)?.deckKey(),
        deck.getOrNull(1)?.deckKey(),
        deck.getOrNull(2)?.deckKey(),
    ) {
        deck.take(3).forEach { card ->
            flatSearchContainer.store.intent(
                FlatListIntent.PrefetchDetail(
                    flatPlatform = card.flatPlatform,
                    adId = card.adId,
                    markAsViewed = false,
                )
            )
        }
    }

    PrefetchDeckImages(deck = deck.take(3))

    val cityName = remember(filterRepository.lastFilter()) {
        LocationUiMapper.findSelectedCity(
            filterRepository.lastFilter().location?.city ?: CityCode.MINSK
        ).displayName
    }

    fun dismissCard(flat: UiFlat, outcome: SwipeOutcome) {
        if (pinnedFrontKey == flat.deckKey()) pinnedFrontKey = null
        pendingDismissKeys = pendingDismissKeys + flat.deckKey()
        undoStack = undoStack + SwipeUndoEntry(flat.flatPlatform, flat.adId, outcome)
        when (outcome) {
            SwipeOutcome.Disliked -> flatSearchContainer.store.intent(
                FlatListIntent.SetDisliked(flat.flatPlatform, flat.adId)
            )

            SwipeOutcome.Liked -> flatSearchContainer.store.intent(
                FlatListIntent.ClickOnFavorite(flat.flatPlatform, flat.adId)
            )
        }
        swipeCount += 1
        val interval = monetizationConfig.swipeAdInterval
        if (userTierProvider.shouldShowAds() && interval > 0 && swipeCount % interval == 0) {
            swipeScope.launch {
                adService.showInterstitial(monetizationConfig.interstitialAdUnit)
            }
        }
    }

    fun undoLastSwipe() {
        val entry = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        pendingDismissKeys = pendingDismissKeys - entry.deckKey()
        pinnedFrontKey = entry.deckKey()
        swipeProgress = 0f
        stackPromoteProgress = 0f
        when (entry.outcome) {
            SwipeOutcome.Disliked -> flatSearchContainer.store.intent(
                FlatListIntent.ClearDislike(entry.flatPlatform, entry.adId)
            )

            SwipeOutcome.Liked -> flatSearchContainer.store.intent(
                // Toggle favorite off (same intent as heart tap).
                FlatListIntent.ClickOnFavorite(entry.flatPlatform, entry.adId)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        premiumBanner?.let { banner ->
            Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                banner()
            }
        }
        when {
            isSearchLoading && deck.isEmpty() -> {
                Box(Modifier.fillMaxSize()) {
                    SwipeSearchProgressBar(Modifier.align(Alignment.TopCenter))
                }
            }

            deck.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(bottom = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyScreenContent(
                        modifier = Modifier.fillMaxWidth(),
                        stringResource = Res.string.no_data_available,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = filterScreenState.filters.getActiveFiltersText(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                val premiumTopPad = if (premiumBanner != null) {
                    FlatHubTheme.dimens.stickyBarHeight
                } else {
                    0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = premiumTopPad),
                ) {
                    val visible = deck.take(3)
                    val frontKey = visible.firstOrNull()?.deckKey()
                    LaunchedEffect(frontKey) {
                        swipeProgress = 0f
                        stackPromoteProgress = 0f
                    }
                    for (i in visible.lastIndex downTo 0) {
                        val flat = visible[i]
                        val isFront = i == 0
                        val stackTransform = stackCardTransform(
                            depth = i,
                            promoteProgress = stackPromoteProgress,
                        )
                        key(flat.deckKey()) {
                            SwipeableCard(
                                enabled = isFront,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = stackTransform.scale
                                        scaleY = stackTransform.scale
                                        translationY = stackTransform.translationY
                                        alpha = stackTransform.alpha
                                    },
                                onSwipedLeft = {
                                    stackPromoteProgress = 1f
                                    swipeProgress = 0f
                                    dismissCard(flat, SwipeOutcome.Disliked)
                                },
                                onSwipedRight = {
                                    stackPromoteProgress = 1f
                                    swipeProgress = 0f
                                    dismissCard(flat, SwipeOutcome.Liked)
                                },
                                onSwipeProgress = { progress ->
                                    if (isFront) {
                                        swipeProgress = progress
                                        stackPromoteProgress = abs(progress.coerceIn(-1f, 1f))
                                    }
                                },
                            ) {
                                TwinbyCardFace(
                                    flat = flat,
                                    cityName = cityName,
                                    swipeProgress = if (isFront) swipeProgress else 0f,
                                    interactive = isFront,
                                    showSearchProgress = isFront && isSearchLoading,
                                    onOpenDetail = {
                                        pinnedFrontKey = flat.deckKey()
                                        flatSearchContainer.store.intent(
                                            FlatListIntent.OpenDetail(
                                                flatPlatform = flat.flatPlatform,
                                                adId = flat.adId,
                                                markAsViewedOnOpen = false,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        val fabMargin = FlatHubTheme.dimens.fabMargin
        if (undoStack.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(fabMargin),
            ) {
                FlatZenFloatingActionButton(
                    onClick = ::undoLastSwipe,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(fabMargin),
        ) {
            FilterActionButton(
                onClick = { flatSearchContainer.store.intent(FlatListIntent.OpenFilter) },
                isAnyFilterApplied = listState.isAnyFilterApplied,
            )
        }
    }
}

/** Same loading bar as MapScreen. */
@Composable
private fun SwipeSearchProgressBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .padding(4.dp)
    )
}

@Composable
private fun PrefetchDeckImages(deck: List<UiFlat>) {
    val context = LocalPlatformContext.current
    val urls = remember(deck) {
        buildList {
            deck.forEachIndexed { index, flat ->
                val photos = flat.imageUrls
                if (photos.isEmpty()) return@forEachIndexed
                if (index == 0) {
                    // Current card: warm a few photos ahead for fast taps.
                    addAll(photos.take(4))
                } else {
                    // Back cards: first photo only.
                    add(photos.first())
                }
            }
        }.distinct()
    }
    LaunchedEffect(urls) {
        val loader = context.imageLoader
        urls.forEach { url ->
            if (url.isBlank()) return@forEach
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TwinbyCardFace(
    flat: UiFlat,
    cityName: String,
    swipeProgress: Float,
    interactive: Boolean,
    showSearchProgress: Boolean,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoIndex by remember(flat.adId, flat.imageUrls.size) {
        mutableIntStateOf(0)
    }
    val photos = flat.imageUrls
    val safeIndex = if (photos.isEmpty()) 0 else photoIndex.coerceIn(0, photos.lastIndex)
    val context = LocalPlatformContext.current

    // Prefetch neighbors of the active photo into memory cache.
    LaunchedEffect(photos, safeIndex) {
        if (photos.isEmpty()) return@LaunchedEffect
        val loader = context.imageLoader
        listOfNotNull(
            photos.getOrNull(safeIndex),
            photos.getOrNull(safeIndex + 1),
            photos.getOrNull(safeIndex + 2),
            photos.getOrNull(safeIndex - 1),
        ).distinct().forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = FlatHubTheme.shapes.none,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (photos.isNotEmpty()) {
                SwipeCardPhoto(
                    imageUrl = photos[safeIndex],
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }

            if (interactive) {
                val noClickIndication = null
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = noClickIndication,
                            ) {
                                if (photos.isNotEmpty()) {
                                    photoIndex = (safeIndex - 1).coerceAtLeast(0)
                                }
                            },
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.30f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = noClickIndication,
                                onClick = onOpenDetail,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = noClickIndication,
                            ) {
                                if (photos.isNotEmpty()) {
                                    photoIndex = (safeIndex + 1).coerceAtMost(photos.lastIndex)
                                }
                            },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(FlatHubTheme.dimens.screenHorizontalCompact),
            ) {
                if (showSearchProgress) {
                    SwipeSearchProgressBar()
                } else {
                    PhotoStepBar(
                        count = photos.size.coerceAtLeast(1),
                        activeIndex = if (photos.isEmpty()) 0 else safeIndex,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val primaryChipStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        flat.priceText.mainPriceText.takeIf { it.isNotBlank() }?.let {
                            FlatZenOverlayChip(text = it, textStyle = primaryChipStyle)
                        }
                        flat.publishedAt?.takeIf { it.isNotBlank() }?.let {
                            FlatZenOverlayChip(text = it, textStyle = primaryChipStyle)
                        }
                        flat.numberOfRooms?.let { rooms ->
                            FlatZenOverlayChip(
                                text = localizedRoomsLabel(rooms),
                                textStyle = primaryChipStyle,
                            )
                        }
                        flat.totalArea?.let {
                            FlatZenOverlayChip(
                                text = localizedArea(it),
                                textStyle = primaryChipStyle,
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FlatZenOverlayChip(cityName)
                        if (flat.address.isNotBlank()) FlatZenOverlayChip(flat.address)
                        flat.metroStation?.let { FlatZenOverlayChip(it) }
                    }
                }
            }

            if (flat.description.isNotBlank()) {
                val semantic = FlatHubTheme.semantic
                val fabSafe = FlatHubTheme.dimens.fabSafeZone
                Text(
                    text = flat.description,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = fabSafe, end = fabSafe, bottom = fabSafe, top = 12.dp)
                        .background(semantic.photoOverlayScrim, FlatHubTheme.shapes.extraSmall)
                        .padding(8.dp),
                )
            }

            val likeAlpha = swipeProgress.coerceIn(0f, 1f)
            val dislikeAlpha = (-swipeProgress).coerceIn(0f, 1f)
            if (likeAlpha > 0.05f) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = FlatHubTheme.semantic.swipeLike.copy(alpha = likeAlpha),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .graphicsLayer { rotationZ = -12f },
                )
            }
            if (dislikeAlpha > 0.05f) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = FlatHubTheme.semantic.swipeDislike.copy(alpha = dislikeAlpha),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .graphicsLayer { rotationZ = 12f },
                )
            }
        }
    }
}

@Composable
private fun SwipeCardPhoto(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val imageLoader = context.imageLoader
    val cachedSize = remember(imageUrl, imageLoader) {
        imageLoader.memoryCache
            ?.get(MemoryCache.Key(imageUrl))
            ?.image
            ?.let { IntSize(it.width, it.height) }
    }
    var imageSize by remember(imageUrl) { mutableStateOf(cachedSize) }

    val imageRequest: ImageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val contentScale = remember(imageSize, maxWidth, maxHeight) {
            imageSize?.let { size ->
                swipePhotoContentScale(
                    imageWidth = size.width,
                    imageHeight = size.height,
                    containerWidth = maxWidth.value,
                    containerHeight = maxHeight.value,
                )
            } ?: ContentScale.Fit
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onSuccess = { state ->
                val width = state.result.image.width
                val height = state.result.image.height
                if (imageSize?.width != width || imageSize?.height != height) {
                    imageSize = IntSize(width, height)
                }
            },
        )
    }
}

/**
 * Portrait photos fill the card; landscape photos fit without cropping (like DetailScreen).
 * Container size follows screen orientation via [BoxWithConstraints].
 */
private fun swipePhotoContentScale(
    imageWidth: Int,
    imageHeight: Int,
    containerWidth: Float,
    containerHeight: Float,
): ContentScale {
    if (imageWidth <= 0 || imageHeight <= 0) return ContentScale.Fit

    val imageIsLandscape = imageWidth > imageHeight
    val imageIsPortrait = imageHeight > imageWidth

    return when {
        imageIsPortrait -> ContentScale.Crop
        imageIsLandscape -> ContentScale.Fit
        else -> {
            val containerIsLandscape = containerWidth > containerHeight
            if (containerIsLandscape) ContentScale.Fit else ContentScale.Crop
        }
    }
}

@Composable
private fun PhotoStepBar(count: Int, activeIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(FlatHubTheme.shapes.extraSmall)
                    .background(
                        if (index <= activeIndex) Color.White
                        else Color.White.copy(alpha = 0.35f)
                    ),
            )
        }
    }
}
