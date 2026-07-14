package io.flatzen.viewmodel.swipe

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.tier.MonetizationSessionState
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.viewmodel.list.FlatListIntent
import io.flatzen.viewmodel.list.FlatListScreenState
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.list.UiFlat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import repository.fillter.FilterRepository
import repository.fillter.lastFilter

private typealias SwipeCtx = PipelineContext<SwipeScreenState, SwipeIntent, SwipeAction>

class SwipeContainer(
    private val flatSearchContainer: FlatSearchContainer,
    private val filterRepository: FilterRepository,
    private val userTierProvider: UserTierProvider,
    private val monetizationRemoteConfig: MonetizationRemoteConfig,
    private val adService: AdService,
    private val navigator: FlatHubNavigator,
) : Container<SwipeScreenState, SwipeIntent, SwipeAction> {

    private var flatsSinceLastAd = 0
    private var wasSearchLoading = false
    private var pendingInitialSearch = false
    private var adCooldownJob: Job? = null
    private var nativePrefetchJob: Job? = null

    private companion object {
        /** Match swipe ContentStream batch size / Appodeal native cache cap. */
        const val SWIPE_NATIVE_PREFETCH_COUNT = 5

        /** Start warming creative this many flats before the interval. */
        const val SWIPE_NATIVE_PREFETCH_AHEAD = 6
    }

    override val store = store(initial = SwipeScreenState.Initial) {
        reduce { intent ->
            when (intent) {
                SwipeIntent.ScreenVisible -> handleScreenVisible()
                is SwipeIntent.SyncListState -> handleSyncListState(intent.listState)
                is SwipeIntent.BeginCardDismiss -> handleBeginCardDismiss(intent.deckKey)
                is SwipeIntent.CancelCardDismiss -> handleCancelCardDismiss(intent.deckKey)
                is SwipeIntent.SwipeFlat -> handleSwipeFlat(intent.flat, intent.outcome)
                SwipeIntent.DismissAdCard -> handleDismissAdCard()
                SwipeIntent.UndoLastSwipe -> handleUndoLastSwipe()
                is SwipeIntent.PinFrontCard -> updateState { copy(pinnedFrontKey = intent.deckKey).rebuildDeck() }
                is SwipeIntent.OpenDetail -> handleOpenDetail(intent.flat)
                SwipeIntent.OpenFilter -> navigator.navigate(FlatHubCommand.OpenFilter)
                SwipeIntent.OpenPremium -> navigator.navigate(FlatHubCommand.OpenPremium)
            }
        }
    }

    private suspend fun SwipeCtx.handleScreenVisible() {
        pendingInitialSearch = true
        flatSearchContainer.store.intent(FlatListIntent.ScreenVisible)
        // Warm native fill as soon as swipe opens; show-position logic stays unchanged.
        prefetchSwipeNativeAds()
    }

    private suspend fun SwipeCtx.handleSyncListState(listState: FlatListScreenState) {
        if (pendingInitialSearch && listState.flatList.isEmpty() && !listState.isLoading) {
            pendingInitialSearch = false
            flatSearchContainer.store.intent(FlatListIntent.SearchFlats(isLoadMore = false))
        }

        val isSearchLoading = listState.isLoading || listState.isRefreshing
        val cityName = LocationUiMapper.findSelectedCity(
            filterRepository.lastFilter().location?.city ?: CityCode.MINSK,
        ).displayName

        if (isSearchLoading && !listState.isLoadingMore && !wasSearchLoading) {
            flatsSinceLastAd = 0
            cancelAdCooldownJob()
            updateState {
                copy(
                    isLoading = listState.isLoading,
                    isRefreshing = listState.isRefreshing,
                    isLoadingMore = listState.isLoadingMore,
                    noFlatsToLoadMore = listState.noFlatsToLoadMore,
                    isAnyFilterApplied = listState.isAnyFilterApplied,
                    flatList = listState.flatList,
                    cityName = cityName,
                    pendingDismissKeys = emptySet(),
                    animatingOutKeys = emptySet(),
                    pinnedFrontKey = null,
                    adCardQueued = false,
                    adAnchorFlatKey = null,
                    undoStack = persistentListOf(),
                ).rebuildDeck()
            }
        } else {
            updateState {
                copy(
                    isLoading = listState.isLoading,
                    isRefreshing = listState.isRefreshing,
                    isLoadingMore = listState.isLoadingMore,
                    noFlatsToLoadMore = listState.noFlatsToLoadMore,
                    isAnyFilterApplied = listState.isAnyFilterApplied,
                    flatList = listState.flatList,
                    cityName = cityName,
                )
                    .prunePendingDismissKeys()
                    .rebuildDeck()
            }
        }

        wasSearchLoading = isSearchLoading
        updateState {
            maybeRequestLoadMore(this)
            prefetchTopCards(this)
            this
        }
        scheduleAdWhenCooldownReady()
    }

    private suspend fun SwipeCtx.handleBeginCardDismiss(deckKey: String) {
        val queueAdForSwipeStart = deckKey != SWIPE_AD_DECK_KEY && shouldQueueAdBehindFront()
        updateState {
            val queueNow = queueAdForSwipeStart && !adCardQueued
            copy(
                animatingOutKeys = animatingOutKeys + deckKey,
                adCardQueued = adCardQueued || queueAdForSwipeStart,
                adAnchorFlatKey = when {
                    adAnchorFlatKey != null -> adAnchorFlatKey
                    queueNow -> deckKey
                    else -> null
                },
            ).rebuildDeck()
        }
    }

    private suspend fun SwipeCtx.handleCancelCardDismiss(deckKey: String) {
        updateState {
            copy(animatingOutKeys = animatingOutKeys - deckKey).rebuildDeck()
        }
    }

    private suspend fun SwipeCtx.handleSwipeFlat(flat: UiFlat, outcome: SwipeOutcome) {
        val deckKey = flat.swipeDeckKey()

        when (outcome) {
            SwipeOutcome.Disliked -> flatSearchContainer.store.intent(
                FlatListIntent.SetDisliked(flat.flatPlatform, flat.adId),
            )

            SwipeOutcome.Liked -> flatSearchContainer.store.intent(
                FlatListIntent.ClickOnFavorite(flat.flatPlatform, flat.adId),
            )
        }

        flatsSinceLastAd += 1
        val preQueueAd = shouldPreQueueAdOnSwipeComplete()

        updateState {
            // Apply dismiss first so orderedSwipeFlats() yields the *next* front.
            // Using the old receiver would anchor the ad to the card we just swiped,
            // which makes rebuildDeck() insert Ad at index 0 (instant pop) instead of
            // behind the next flat.
            val wasAlreadyQueued = adCardQueued
            val afterDismiss = copy(
                pinnedFrontKey = pinnedFrontKey?.takeUnless { it == deckKey },
                pendingDismissKeys = pendingDismissKeys + deckKey,
                animatingOutKeys = animatingOutKeys - deckKey,
                undoStack = (undoStack + SwipeUndoEntry(
                    flat.flatPlatform,
                    flat.adId,
                    outcome
                )).toImmutableList(),
                adCardQueued = wasAlreadyQueued || preQueueAd,
            )
            val nextFrontKey = afterDismiss.orderedSwipeFlats().firstOrNull()?.swipeDeckKey()
            val resolvedAnchor = when {
                // Existing anchor still points at a remaining flat — keep it.
                adAnchorFlatKey != null && adAnchorFlatKey != deckKey -> adAnchorFlatKey
                // First pre-queue on this swipe: park Ad behind the new front.
                preQueueAd && !wasAlreadyQueued -> nextFrontKey
                // Anchored flat just left the deck — clear so Ad becomes front.
                adAnchorFlatKey == deckKey -> null
                else -> adAnchorFlatKey
            }
            afterDismiss.copy(adAnchorFlatKey = resolvedAnchor).rebuildDeck()
        }
        if (shouldPrefetchSwipeNativeEarly() || shouldQueueAdBehindFront()) {
            prefetchSwipeNativeAds()
        }
        scheduleAdWhenCooldownReady()
    }

    private suspend fun SwipeCtx.handleDismissAdCard() {
        MonetizationSessionState.recordAdShown()
        flatsSinceLastAd = 0
        cancelAdCooldownJob()
        updateState {
            copy(
                adCardQueued = false,
                adAnchorFlatKey = null,
                animatingOutKeys = animatingOutKeys - SWIPE_AD_DECK_KEY,
            ).rebuildDeck()
        }
        // Next cycle: start filling cache while the minute cooldown runs.
        prefetchSwipeNativeAds()
        scheduleAdWhenCooldownReady()
    }

    private suspend fun SwipeCtx.handleUndoLastSwipe() {
        withState {
            val entry = undoStack.lastOrNull() ?: return@withState
            updateState {
                copy(
                    undoStack = undoStack.dropLast(1).toImmutableList(),
                    pendingDismissKeys = pendingDismissKeys - entry.deckKey(),
                    pinnedFrontKey = entry.deckKey(),
                ).rebuildDeck()
            }

            when (entry.outcome) {
                SwipeOutcome.Disliked -> flatSearchContainer.store.intent(
                    FlatListIntent.ClearDislike(entry.flatPlatform, entry.adId),
                )

                SwipeOutcome.Liked -> flatSearchContainer.store.intent(
                    FlatListIntent.ClickOnFavorite(entry.flatPlatform, entry.adId),
                )
            }
        }
    }

    private fun handleOpenDetail(flat: UiFlat) {
        flatSearchContainer.store.intent(
            FlatListIntent.OpenDetail(
                flatPlatform = flat.flatPlatform,
                adId = flat.adId,
                markAsViewedOnOpen = false,
            ),
        )
    }

    private fun isAdPreQueueEligible(): Boolean {
        if (!userTierProvider.shouldShowAds()) return false
        val interval = monetizationRemoteConfig.swipeAdInterval
        return interval > 0 && flatsSinceLastAd >= interval - 1
    }

    private fun swipeAdMinIntervalMinutes(): Long =
        monetizationRemoteConfig.swipeAdMinIntervalMinutes

    /**
     * Queue ad behind the current top flat once the swipe threshold is reached and cooldown allows.
     */
    private fun shouldQueueAdBehindFront(): Boolean {
        if (!isAdPreQueueEligible()) return false
        return MonetizationSessionState.canShowAd(swipeAdMinIntervalMinutes())
    }

    /**
     * After at least (interval - 1) swipes: pre-queue ad behind the next top card.
     * Uses >= so a delayed cooldown queue still lands behind the current front.
     */
    private fun shouldPreQueueAdOnSwipeComplete(): Boolean {
        val interval = monetizationRemoteConfig.swipeAdInterval
        return shouldQueueAdBehindFront() && flatsSinceLastAd >= interval - 1
    }

    private fun cancelAdCooldownJob() {
        adCooldownJob?.cancel()
        adCooldownJob = null
    }

    private suspend fun SwipeCtx.scheduleAdWhenCooldownReady() {
        if (!isAdPreQueueEligible()) {
            cancelAdCooldownJob()
            return
        }

        if (shouldQueueAdBehindFront()) {
            cancelAdCooldownJob()
            prefetchSwipeNativeAds()
            maybeQueueAdCard()
            return
        }

        cancelAdCooldownJob()
        val minInterval = swipeAdMinIntervalMinutes()
        // Warm creative during the wait so the card can show a ready fill at reveal.
        prefetchSwipeNativeAds()
        adCooldownJob = launch {
            val waitMs = MonetizationSessionState.millisUntilCanShowAd(minInterval)
            if (waitMs > 0) delay(waitMs)
            prefetchSwipeNativeAds()
            if (shouldQueueAdBehindFront()) {
                maybeQueueAdCard()
            }
        }
    }

    private suspend fun SwipeCtx.maybeQueueAdCard() {
        if (!shouldQueueAdBehindFront()) return
        updateState {
            if (adCardQueued) {
                this
            } else {
                copy(
                    adCardQueued = true,
                    adAnchorFlatKey = adAnchorFlatKey ?: orderedSwipeFlats().firstOrNull()
                        ?.swipeDeckKey(),
                ).rebuildDeck()
            }
        }
    }

    /**
     * Loads native creatives into Appodeal cache without inserting [SwipeDeckItem.Ad].
     * Display still gated by interval / cooldown via [adCardQueued].
     */
    private fun SwipeCtx.prefetchSwipeNativeAds() {
        if (!userTierProvider.shouldShowAds()) return
        val placement = monetizationRemoteConfig.swipeCardPlacement
        if (placement.isBlank() || !adService.isInitialized()) return
        nativePrefetchJob?.cancel()
        nativePrefetchJob = launch {
            adService.prefetchNative(placement, SWIPE_NATIVE_PREFETCH_COUNT)
        }
    }

    private fun shouldPrefetchSwipeNativeEarly(): Boolean {
        if (!userTierProvider.shouldShowAds()) return false
        val interval = monetizationRemoteConfig.swipeAdInterval
        if (interval <= 0) return false
        val threshold = (interval - SWIPE_NATIVE_PREFETCH_AHEAD).coerceAtLeast(0)
        return flatsSinceLastAd >= threshold
    }

    private fun maybeRequestLoadMore(swipeState: SwipeScreenState) {
        if (swipeState.flatDeckSize <= 3 &&
            !swipeState.isLoadingMore &&
            !swipeState.noFlatsToLoadMore &&
            !swipeState.isLoading
        ) {
            flatSearchContainer.store.intent(FlatListIntent.SearchFlats(isLoadMore = true))
        }
    }

    private fun prefetchTopCards(swipeState: SwipeScreenState) {
        swipeState.deck.take(3).forEach { item ->
            if (item !is SwipeDeckItem.Flat) return@forEach
            val card = item.value
            flatSearchContainer.store.intent(
                FlatListIntent.PrefetchDetail(
                    flatPlatform = card.flatPlatform,
                    adId = card.adId,
                    markAsViewed = false,
                ),
            )
        }
    }

    private fun SwipeScreenState.prunePendingDismissKeys(): SwipeScreenState {
        if (pendingDismissKeys.isEmpty()) return this
        val pruned = pendingDismissKeys.filter { key ->
            val flat = flatList.find { it.swipeDeckKey() == key } ?: return@filter false
            !flat.savedInFavorite && !flat.isViewed && !flat.disliked
        }.toSet()
        return if (pruned == pendingDismissKeys) this else copy(pendingDismissKeys = pruned)
    }

    private fun SwipeScreenState.orderedSwipeFlats(): List<UiFlat> {
        val swipeDeck = flatList.filter { flat ->
            val key = flat.swipeDeckKey()
            val keepWhileAnimating = key in animatingOutKeys
            (keepWhileAnimating || (!flat.savedInFavorite && !flat.isViewed && !flat.disliked)) &&
                    key !in pendingDismissKeys
        }
        val pinned = pinnedFrontKey?.let { key ->
            flatList.find { it.swipeDeckKey() == key && it.swipeDeckKey() !in pendingDismissKeys }
        }
        return when {
            pinned == null -> swipeDeck
            else -> listOf(pinned) + swipeDeck.filter { it.swipeDeckKey() != pinned.swipeDeckKey() }
        }
    }

    private fun SwipeScreenState.rebuildDeck(): SwipeScreenState {
        val orderedFlats = orderedSwipeFlats()
        val items = orderedFlats.map { SwipeDeckItem.Flat(it) }.toMutableList<SwipeDeckItem>()
        val adAnimatingOut = SWIPE_AD_DECK_KEY in animatingOutKeys
        if (adCardQueued || adAnimatingOut) {
            val frontKey = orderedFlats.firstOrNull()?.swipeDeckKey()
            // Keep Ad behind the current top flat while that flat is the anchor
            // or still flying out. Once the anchored flat is gone from the deck,
            // Ad becomes the front card.
            val keepBehindFront = !adAnimatingOut &&
                    frontKey != null &&
                    (frontKey in animatingOutKeys || frontKey == adAnchorFlatKey)
            val insertIndex = when {
                items.isEmpty() || adAnimatingOut -> 0
                keepBehindFront -> 1.coerceAtMost(items.size)
                else -> 0
            }
            items.add(insertIndex, SwipeDeckItem.Ad)
        }
        return copy(deck = items.toImmutableList())
    }
}
