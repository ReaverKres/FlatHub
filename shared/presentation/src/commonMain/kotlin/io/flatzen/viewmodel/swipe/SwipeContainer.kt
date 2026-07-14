package io.flatzen.viewmodel.swipe

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.mappers.LocationUiMapper
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
    private val navigator: FlatHubNavigator,
) : Container<SwipeScreenState, SwipeIntent, SwipeAction> {

    private var flatsSinceLastAd = 0
    private var wasSearchLoading = false
    private var pendingInitialSearch = false
    private var adCooldownJob: Job? = null

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
            val queueNow = preQueueAd && !adCardQueued
            copy(
                pinnedFrontKey = pinnedFrontKey?.takeUnless { it == deckKey },
                pendingDismissKeys = pendingDismissKeys + deckKey,
                animatingOutKeys = animatingOutKeys - deckKey,
                undoStack = (undoStack + SwipeUndoEntry(
                    flat.flatPlatform,
                    flat.adId,
                    outcome
                )).toImmutableList(),
                adCardQueued = adCardQueued || preQueueAd,
                adAnchorFlatKey = when {
                    adAnchorFlatKey != null -> adAnchorFlatKey
                    queueNow -> orderedSwipeFlats().firstOrNull()?.swipeDeckKey()
                    else -> null
                },
            ).rebuildDeck()
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
            maybeQueueAdCard()
            return
        }

        cancelAdCooldownJob()
        val minInterval = swipeAdMinIntervalMinutes()
        adCooldownJob = launch {
            val waitMs = MonetizationSessionState.millisUntilCanShowAd(minInterval)
            if (waitMs > 0) delay(waitMs)
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
