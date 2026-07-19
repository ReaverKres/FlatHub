package io.flatzen.screens.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.no_data_available
import flatzen.composeapp.generated.resources.swipe_ad_label
import io.flatzen.ads.MAX_NATIVE_ADS_PER_BATCH
import io.flatzen.ads.NativeAdMinHeight
import io.flatzen.ads.NativeAdSlot
import io.flatzen.ads.NativeAdSlotStyle
import io.flatzen.ads.clearNativeAdBatch
import io.flatzen.common.localization.localizedArea
import io.flatzen.common.localization.localizedRoomsLabel
import io.flatzen.di.container
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.themes.FlatHubTheme
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.swipe.SWIPE_AD_DECK_KEY
import io.flatzen.viewmodel.swipe.SwipeContainer
import io.flatzen.viewmodel.swipe.SwipeDeckItem
import io.flatzen.viewmodel.swipe.SwipeIntent
import io.flatzen.viewmodel.swipe.SwipeOutcome
import io.flatzen.viewmodel.swipe.swipeDeckKey
import io.flatzen.widgets.FilterActionButton
import io.flatzen.widgets.FlatZenFloatingActionButton
import io.flatzen.widgets.FlatZenOverlayChip
import io.flatzen.widgets.PhotoStepBar
import io.flatzen.widgets.PhotoTapZones
import io.flatzen.widgets.PremiumUpsellCardBanner
import io.flatzen.widgets.PremiumUpsellState
import io.flatzen.widgets.SwipeableCard
import io.flatzen.widgets.rememberPremiumUpsellState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.subscribe
import kotlin.math.abs

private const val STACK_SCALE_STEP = 0.04f
private const val STACK_Y_STEP = 10f
private const val STACK_ALPHA_STEP = 0.08f
private val SwipeNativeAdHeightEstimate = NativeAdMinHeight

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

@Composable
fun SwipeScreen() {
    val swipeContainer: SwipeContainer = container()
    val flatSearchContainer: FlatSearchContainer = koinInject()
    val listState by flatSearchContainer.store.subscribe { }
    val state by swipeContainer.store.subscribe { }
    val filterContainer: FilterContainer = container()
    val filterScreenState by filterContainer.store.subscribe { }
    val premiumUpsell = rememberPremiumUpsellState(
        navigateToPremium = { swipeContainer.store.intent(SwipeIntent.OpenPremium) },
    )

    val monetizationConfig: MonetizationRemoteConfig = koinInject()

    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var stackPromoteProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        swipeContainer.store.intent(SwipeIntent.ScreenVisible)
    }

    LaunchedEffect(listState) {
        swipeContainer.store.intent(SwipeIntent.SyncListState(listState))
    }

    PrefetchDeckImages(
        deck = state.deck.take(3).mapNotNull { (it as? SwipeDeckItem.Flat)?.value },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isSearchLoading && state.deck.isEmpty() -> {
                Box(Modifier.fillMaxSize()) {
                    SwipeSearchProgressBar(Modifier.align(Alignment.TopCenter))
                }
            }

            state.deck.isEmpty() -> {
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
                SwipeCardStack(
                    deck = state.deck,
                    swipeAdPlacement = monetizationConfig.swipeCardPlacement,
                    cityName = state.cityName,
                    isSearchLoading = state.isSearchLoading,
                    premiumUpsell = premiumUpsell,
                    swipeProgress = swipeProgress,
                    stackPromoteProgress = stackPromoteProgress,
                    onSwipeProgressChange = { progress, promote ->
                        swipeProgress = progress
                        stackPromoteProgress = promote
                    },
                    onResetSwipeProgress = {
                        swipeProgress = 0f
                        stackPromoteProgress = 0f
                    },
                    onBeginCardDismiss = { deckKey ->
                        swipeContainer.store.intent(SwipeIntent.BeginCardDismiss(deckKey))
                    },
                    onCancelCardDismiss = { deckKey ->
                        swipeContainer.store.intent(SwipeIntent.CancelCardDismiss(deckKey))
                    },
                    onSwipeFlat = { flat, outcome ->
                        swipeContainer.store.intent(SwipeIntent.SwipeFlat(flat, outcome))
                    },
                    onDismissAd = {
                        swipeContainer.store.intent(SwipeIntent.DismissAdCard)
                    },
                    onOpenDetail = { flat ->
                        swipeContainer.store.intent(SwipeIntent.PinFrontCard(flat.swipeDeckKey()))
                        swipeContainer.store.intent(SwipeIntent.OpenDetail(flat))
                    },
                )
            }
        }

        val fabMargin = FlatHubTheme.dimens.fabMargin
        if (state.showUndo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(fabMargin),
            ) {
                FlatZenFloatingActionButton(
                    onClick = {
                        swipeContainer.store.intent(SwipeIntent.UndoLastSwipe)
                        swipeProgress = 0f
                        stackPromoteProgress = 0f
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                )
            }
        }

        if (!state.showOnboarding) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(fabMargin),
            ) {
                FilterActionButton(
                    onClick = { swipeContainer.store.intent(SwipeIntent.OpenFilter) },
                    isAnyFilterApplied = state.isAnyFilterApplied,
                )
            }
        }

        if (state.showOnboarding) {
            SwipeOnboardingOverlay(
                onComplete = {
                    swipeContainer.store.intent(SwipeIntent.CompleteOnboarding)
                },
            )
        }
    }
}

@Composable
private fun SwipeCardStack(
    deck: List<SwipeDeckItem>,
    swipeAdPlacement: String,
    cityName: String,
    isSearchLoading: Boolean,
    premiumUpsell: PremiumUpsellState?,
    swipeProgress: Float,
    stackPromoteProgress: Float,
    onSwipeProgressChange: (progress: Float, promote: Float) -> Unit,
    onResetSwipeProgress: () -> Unit,
    onBeginCardDismiss: (deckKey: String) -> Unit,
    onCancelCardDismiss: (deckKey: String) -> Unit,
    onSwipeFlat: (UiFlat, SwipeOutcome) -> Unit,
    onDismissAd: () -> Unit,
    onOpenDetail: (UiFlat) -> Unit,
) {
    val visible = deck.take(3)
    val frontKey = visible.firstOrNull()?.deckKey()

    LaunchedEffect(frontKey) {
        onResetSwipeProgress()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in visible.lastIndex downTo 0) {
            val item = visible[i]
            val isFront = i == 0
            val stackTransform = stackCardTransform(
                depth = i,
                promoteProgress = stackPromoteProgress,
            )
            key(item.deckKey()) {
                when (item) {
                    is SwipeDeckItem.Flat -> {
                        val flat = item.value
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
                            onSwipeWillDismiss = { onBeginCardDismiss(flat.swipeDeckKey()) },
                            onSwipeDismissCancelled = {
                                onCancelCardDismiss(flat.swipeDeckKey())
                            },
                            onSwipedLeft = {
                                onSwipeFlat(flat, SwipeOutcome.Disliked)
                                onResetSwipeProgress()
                            },
                            onSwipedRight = {
                                onSwipeFlat(flat, SwipeOutcome.Liked)
                                onResetSwipeProgress()
                            },
                            onSwipeProgress = { progress ->
                                if (isFront) {
                                    onSwipeProgressChange(
                                        progress,
                                        abs(progress.coerceIn(-1f, 1f)),
                                    )
                                }
                            },
                        ) {
                            TwinbyCardFace(
                                flat = flat,
                                cityName = cityName,
                                swipeProgress = if (isFront) swipeProgress else 0f,
                                interactive = isFront,
                                showSearchProgress = isFront && isSearchLoading,
                                premiumUpsell = premiumUpsell,
                                onOpenDetail = { onOpenDetail(flat) },
                            )
                        }
                    }

                    SwipeDeckItem.Ad -> {
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
                            onSwipeWillDismiss = { onBeginCardDismiss(SWIPE_AD_DECK_KEY) },
                            onSwipeDismissCancelled = {
                                onCancelCardDismiss(SWIPE_AD_DECK_KEY)
                            },
                            onSwipedLeft = {
                                onDismissAd()
                                onResetSwipeProgress()
                            },
                            onSwipedRight = {
                                onDismissAd()
                                onResetSwipeProgress()
                            },
                            onSwipeProgress = { progress ->
                                if (isFront) {
                                    onSwipeProgressChange(
                                        progress,
                                        abs(progress.coerceIn(-1f, 1f)),
                                    )
                                }
                            },
                        ) {
                            SwipeAdCardFace(placement = swipeAdPlacement)
                        }
                    }
                }
            }
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
private fun SwipeAdCardFace(
    placement: String,
    modifier: Modifier = Modifier,
) {
    val batchId = remember(placement) { "$placement-${kotlin.random.Random.nextLong()}" }
    val density = LocalDensity.current

    DisposableEffect(batchId) {
        onDispose { clearNativeAdBatch(batchId) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = FlatHubTheme.shapes.none,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxHeightPx = with(density) { maxHeight.roundToPx() }
            val estimatedSlotCount = remember(maxHeightPx) {
                (maxHeight / SwipeNativeAdHeightEstimate)
                    .toInt()
                    .coerceIn(1, MAX_NATIVE_ADS_PER_BATCH)
            }
            var slotCount by remember(maxHeightPx) { mutableIntStateOf(estimatedSlotCount) }
            var failedSlots by remember { mutableStateOf(setOf<Int>()) }
            var measuredHeightsPx by remember { mutableStateOf(IntArray(estimatedSlotCount)) }

            LaunchedEffect(slotCount) {
                if (measuredHeightsPx.size != slotCount) {
                    measuredHeightsPx = IntArray(slotCount)
                }
            }

            val activeSlots = (0 until slotCount).filter { it !in failedSlots }
            val totalMeasuredPx = activeSlots.sumOf { index ->
                measuredHeightsPx.getOrElse(index) { 0 }
            }
            val measuredActiveCount = activeSlots.count { index ->
                measuredHeightsPx.getOrElse(index) { 0 } > 0
            }
            val canGrowByMeasurement = activeSlots.isNotEmpty() &&
                    measuredActiveCount == activeSlots.size &&
                    totalMeasuredPx > 0 &&
                    totalMeasuredPx < maxHeightPx

            LaunchedEffect(
                totalMeasuredPx,
                maxHeightPx,
                slotCount,
                canGrowByMeasurement,
                failedSlots
            ) {
                if (canGrowByMeasurement && slotCount < MAX_NATIVE_ADS_PER_BATCH) {
                    slotCount++
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = FlatHubTheme.dimens.screenHorizontalCompact),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = stringResource(Res.string.swipe_ad_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = FlatHubTheme.dimens.screenHorizontalCompact,
                        end = FlatHubTheme.dimens.screenHorizontalCompact,
                        bottom = 8.dp,
                    ),
                )
                activeSlots.forEach { index ->
                    key(batchId, index) {
                        NativeAdSlot(
                            placement = placement,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .onSizeChanged { size ->
                                    if (index < measuredHeightsPx.size) {
                                        measuredHeightsPx = measuredHeightsPx.copyOf().also {
                                            it[index] = size.height
                                        }
                                    }
                                },
                            style = NativeAdSlotStyle.ContentStream,
                            batchId = batchId,
                            slotIndex = index,
                            batchSize = slotCount,
                            onAdLoadResult = { loaded ->
                                if (!loaded) {
                                    failedSlots += index
                                }
                            },
                        )
                    }
                }
            }
        }
    }
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
                    addAll(photos.take(4))
                } else {
                    add(photos.first())
                }
            }
        }.distinct()
    }
    LaunchedEffect(urls) {
        val loader = SingletonImageLoader.get(context)
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
    premiumUpsell: PremiumUpsellState? = null,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoIndex by remember(flat.adId, flat.imageUrls.size) {
        mutableIntStateOf(0)
    }
    val photos = flat.imageUrls
    val safeIndex = if (photos.isEmpty()) 0 else photoIndex.coerceIn(0, photos.lastIndex)
    val context = LocalPlatformContext.current

    LaunchedEffect(photos, safeIndex) {
        if (photos.isEmpty()) return@LaunchedEffect
        val loader = SingletonImageLoader.get(context)
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
                PhotoTapZones(
                    modifier = Modifier.fillMaxSize(),
                    onTapLeft = {
                        if (photos.isNotEmpty()) {
                            photoIndex = (safeIndex - 1).coerceAtLeast(0)
                        }
                    },
                    onTapCenter = onOpenDetail,
                    onTapRight = {
                        if (photos.isNotEmpty()) {
                            photoIndex = (safeIndex + 1).coerceAtMost(photos.lastIndex)
                        }
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(FlatHubTheme.dimens.screenHorizontalCompact),
            ) {
                premiumUpsell?.let { upsell ->
                    PremiumUpsellCardBanner(state = upsell)
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
                val horizontalPadding = FlatHubTheme.dimens.screenHorizontalCompact
                val fabSafe = (FlatHubTheme.dimens.fabSafeZone.value * 1.2).toInt().dp
                Text(
                    text = flat.description,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = fabSafe,
                            top = 12.dp,
                        )
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
    val imageLoader = SingletonImageLoader.get(context)
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
        modifier = modifier.background(MaterialTheme.colorScheme.background),
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
