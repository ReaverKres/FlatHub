package io.flatzen.viewmodel.list

import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.analytics.Analytics
import io.flatzen.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.marketCountry
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.tier.UserTier
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.monetization.tier.applyFeedDelayFilter
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository
import repository.userpreferences.UserPreferencesRepository
import kotlin.time.Clock

private typealias PipeCtx = PipelineContext<FlatListScreenState, FlatListIntent, FlatListAction>

class FlatSearchContainer(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val connectionMonitor: ConnectionMonitor,
    private val analytics: Analytics,
    private val configFieldsChecker: ConfigFieldsChecker,
    private val userTierProvider: UserTierProvider,
    private val navigator: FlatHubNavigator,
) : Container<FlatListScreenState, FlatListIntent, FlatListAction> {

    private var noFlatsToLoadMore: Boolean = false
    private var isNetworkAvailable: Boolean = true

    /** Survives UI remounts (e.g. language key); only newer search generations clear loading. */
    private var searchJob: Job? = null
    private var searchGeneration: Int = 0

    override val store = store(
        initial = FlatListScreenState.Initial
    ) {
        // Store-scoped: must not run inside whileSubscribed — language remount cancels that
        // scope mid-flight and leaves isLoading=true with an empty list.
        init {
            launch {
                filterRepository.forceReloadFlow.collect {
                    noFlatsToLoadMore = false
                    launchSearch { loadAllFlatsFromFilterUpdate() }
                }
            }
        }
        whileSubscribed {
            connectionMonitor.isNetworkAvailable
                .onEach { isNetworkAvailable = it }
                .flowOn(Dispatchers.Default)
                .collect { }
        }
        whileSubscribed {
            filterRepository.cashedFilterFlow
                .collect { newFilters ->
                    noFlatsToLoadMore = false
                    updateState { copy(isAnyFilterApplied = newFilters.commonFilterRequestModel != CommonFilterRequestModel()) }
                }
        }
        whileSubscribed {
            mergedRepository.getAllFlatsFromLocalDb()
                .asLCE()
                .flowOn(Dispatchers.IO)
                .collect { lce ->
                    handleLoadDbFlatsFromFlow(lce)
                }
        }

        reduce { intent ->
            when (intent) {
                FlatListIntent.ScrollToTop -> action(FlatListAction.ScrollToTopEffect)

                FlatListIntent.ScreenVisible -> handleScreenVisible()

                is FlatListIntent.IsAnyFilterAppliedCheck -> onIsAnyFilterAppliedCheck(intent)

                is FlatListIntent.SearchFlats -> handleSearchFlats(intent)

                is FlatListIntent.ClickOnFavorite -> handleClickOnFavorite(intent)
                is FlatListIntent.ClearDislike -> handleClearDislike(intent)
                is FlatListIntent.SetDisliked -> handleSetDisliked(intent)
                is FlatListIntent.PrefetchDetail -> handlePrefetchDetail(intent)

                is FlatListIntent.LoadDbFlats -> handleLoadDbFlats(intent)

                is FlatListIntent.TrackScreenView -> onTrackScreenView(intent)

                FlatListIntent.ToggleView -> handleToggleView()
                is FlatListIntent.SetListView -> handleSetListView(intent)
                is FlatListIntent.HideNetworkErrorDialog -> onHideNetworkErrorDialog(intent)

                is FlatListIntent.OpenDetail -> onOpenDetail(intent)

                FlatListIntent.OpenFilter -> navigator.navigate(FlatHubCommand.OpenFilter)
                FlatListIntent.OpenNotifications -> navigator.navigate(FlatHubCommand.OpenNotifications())
                FlatListIntent.OpenPremium -> navigator.navigate(FlatHubCommand.OpenPremium)
            }
        }
    }

    private suspend fun PipeCtx.onIsAnyFilterAppliedCheck(
        intent: FlatListIntent.IsAnyFilterAppliedCheck,
    ) {
        updateState { copy(isAnyFilterApplied = intent.applied) }
    }

    private fun PipeCtx.onOpenDetail(intent: FlatListIntent.OpenDetail) {
        navigator.navigate(
            FlatHubCommand.OpenDetail(
                platform = intent.flatPlatform,
                objectId = intent.adId,
                markAsViewedOnOpen = intent.markAsViewedOnOpen,
            ),
        )
    }

    private fun PipeCtx.onTrackScreenView(intent: FlatListIntent.TrackScreenView) {
        launch(Dispatchers.IO) {
            analytics.track(
                AnalyticsEvent(
                    eventName = AppMetrcica.Events.SCREEN_VIEW,
                    parameters = mapOf(
                        AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                        AppMetrcica.Parameters.TIMESTAMP to Clock.System.now(),
                    ) + intent.parameters,
                ),
            )
        }
    }

    private suspend fun PipeCtx.onHideNetworkErrorDialog(intent: FlatListIntent.HideNetworkErrorDialog) {
        if (intent.dontShowAgain) {
            var fingerprint = ""
            withState { fingerprint = errorDialogState?.fingerprint.orEmpty() }
            SearchNetworkErrorDismissStore.suppress(fingerprint)
        }
        updateState { copy(errorDialogState = null) }
    }

    /**
     * Runs search on the store pipeline (survives Home unsubscribe / locale remount).
     * Cancels the previous search; only the active generation clears loading on cancel.
     */
    private fun PipeCtx.launchSearch(block: suspend PipeCtx.() -> Unit) {
        searchJob?.cancel()
        val generation = ++searchGeneration
        searchJob = launch {
            try {
                block()
            } catch (e: CancellationException) {
                if (generation == searchGeneration) {
                    clearLoadingFlags()
                }
                throw e
            }
        }
    }

    private suspend fun PipeCtx.clearLoadingFlags() {
        updateState {
            copy(isLoading = false, isLoadingMore = false, isRefreshing = false)
        }
    }

    private suspend fun PipeCtx.loadAllFlatsFromFilterUpdate() {
        if (connectionMonitor.isNetworkAvailable.first().not()) return
        if (configFieldsChecker.checkBoolean(ConfigFields.FreeVersionAvailable)?.not() == true) {
            updateState {
                copy(
                    infoDialogState = InfoDialogState(
                        isVisible = true,
                        dialogType = DialogType.ForceUpdate,
                        title = LocalizationKeys.FORCE_UPDATE_TITLE,
                        description = LocalizationKeys.FORCE_UPDATE_DESCRIPTION
                    )
                )
            }
            return
        }
        if (noFlatsToLoadMore) return
        filterRepository.currentHomePage = 1
        mergedRepository.clearCashedFlats()
        action(FlatListAction.ScrollToTopEffect)
        loadAllFlats(false, false)
        // After list replace LazyList can restore the old index — scroll again.
        action(FlatListAction.ScrollToTopEffect)
    }

    private suspend fun PipeCtx.handleLoadDbFlatsFromFlow(lce: LCE<List<AppFlat>>) {
        when (lce) {
            is LCE.Loading -> {}
            is LCE.Error -> {}
            is LCE.Content -> {
                val dbFlats = lce.value
                if (isNetworkAvailable) {
                    updateState {
                        copy(
                            flatList = flatList.map { flatOnScreen ->
                                val flatFromDb = dbFlats.find { f ->
                                    f.adId == flatOnScreen.adId && f.flatPlatform == flatOnScreen.flatPlatform
                                }
                                if (flatFromDb != null) {
                                    flatOnScreen.copy(
                                        savedInFavorite = flatFromDb.savedInFavorites,
                                        isViewed = flatFromDb.isViewed,
                                        disliked = flatFromDb.dislike,
                                        description = flatFromDb.description.orEmpty().ifEmpty {
                                            flatOnScreen.description
                                        },
                                        imageUrls = (flatFromDb.imageUrls ?: flatOnScreen.imageUrls)
                                            .toImmutableList(),
                                        // CoordEnricher writes lat/lng to Room; map markers need this.
                                        coordinates = flatFromDb.coordinates
                                            ?: flatOnScreen.coordinates,
                                        isDetailDataLoaded = flatFromDb.flatDevInfo.isDetailLoaded ||
                                                flatOnScreen.isDetailDataLoaded,
                                    )
                                } else flatOnScreen
                            }.toImmutableList()
                        )
                    }
                } else {
                    // Offline: Room still holds previous countries' ads — keep current market only.
                    applyFlatsLoaded(dbFlats.filterForCurrentMarket(), false, false)
                }
            }
        }
    }

    private suspend fun PipeCtx.handleScreenVisible() {
        var triggeredNetworkReload = false
        val lastNet = filterRepository.lastNetworkFilter
        val selectedFilter = filterRepository.getSelectedSavedFilter()
        if (selectedFilter != null && selectedFilter.filterData != lastNet) {
            filterRepository.applySavedFilter(selectedFilter, true)
            triggeredNetworkReload = true
        } else {
            if (filterRepository.cashedFilterFlow.replayCache.isEmpty()) {
                filterRepository.updateFilter(CommonFilterRequestModel(), true)
                triggeredNetworkReload = true
            } else {
                val last = filterRepository.lastFilter()
                if (lastNet != last) {
                    filterRepository.updateFilter(last, true)
                    triggeredNetworkReload = true
                }
            }
        }

        // Recover from cancelled mid-flight search (locale remount / unsubscribe)
        // when filters did not change and therefore forceReload was not emitted.
        if (!triggeredNetworkReload) {
            var stuckLoading = false
            withState {
                stuckLoading = isLoading && !isLoadingMore && searchJob?.isActive != true
            }
            if (stuckLoading) {
                launchSearch { loadAllFlatsFromFilterUpdate() }
            }
        }

        userPreferencesRepository.getUserPreferences().firstOrNull()?.let { preferences ->
            updateState { copy(isListView = preferences.isListView) }
        }

        launch(Dispatchers.IO) {
            mergedRepository.cleanupOldFlats()
        }
    }

    private suspend fun PipeCtx.handleSearchFlats(intent: FlatListIntent.SearchFlats) {
        launch(Dispatchers.IO) {
            analytics.track(
                AnalyticsEvent(
                    eventName = "search_flats",
                    parameters = mapOf(
                        "is_load_more" to intent.isLoadMore,
                        "is_refreshing" to intent.isRefreshing,
                        "page" to filterRepository.currentHomePage
                    )
                )
            )
        }

        if (connectionMonitor.isNetworkAvailable.first().not()) return

        if (configFieldsChecker.checkBoolean(ConfigFields.FreeVersionAvailable)?.not() == true) {
            updateState {
                copy(
                    infoDialogState = InfoDialogState(
                        isVisible = true,
                        dialogType = DialogType.ForceUpdate,
                        title = LocalizationKeys.FORCE_UPDATE_TITLE,
                        description = LocalizationKeys.FORCE_UPDATE_DESCRIPTION
                    )
                )
            }
            return
        }

        if (intent.isLoadMoreForce.not() && noFlatsToLoadMore && intent.isRefreshing.not()) return

        if (intent.isRefreshing || intent.isLoadMore.not()) {
            filterRepository.currentHomePage = 1
            mergedRepository.clearCashedFlats()
        }
        if (intent.isLoadMore) {
            filterRepository.currentHomePage++
        }
        if (intent.isLoadMore.not()) {
            action(FlatListAction.ScrollToTopEffect)
        }

        launchSearch {
            loadAllFlats(intent.isLoadMore, intent.isRefreshing)
            if (intent.isLoadMore.not()) {
                action(FlatListAction.ScrollToTopEffect)
            }
        }
    }

    private suspend fun PipeCtx.handleClickOnFavorite(intent: FlatListIntent.ClickOnFavorite) {
        mergedRepository.saveFlatToFavorite(intent.flatPlatform, intent.adId).asLCE()
            .collect { lce ->
                when (lce) {
                    is LCE.Loading -> {
                        var detailFlatAccessible = false
                        withState {
                            detailFlatAccessible = flatList.find {
                                it.adId == intent.adId && it.flatPlatform == intent.flatPlatform
                            }?.isDetailDataLoaded == true
                        }
                        if (!detailFlatAccessible) {
                            updateState {
                                copy(
                                    flatList = flatList.map { uiFlat ->
                                        if (uiFlat.adId == intent.adId && intent.flatPlatform == uiFlat.flatPlatform) {
                                            uiFlat.copy(saveInFavoriteInProgress = true)
                                        } else uiFlat
                                    }.toImmutableList()
                                )
                            }
                        }
                    }

                    is LCE.Error -> {}
                    is LCE.Content -> {
                        val updatedFlat = lce.value
                        updateState {
                            copy(
                                flatList = flatList.map { uiFlat ->
                                    if (uiFlat.adId == updatedFlat?.adId && uiFlat.flatPlatform == updatedFlat.flatPlatform) {
                                        uiFlat.copy(
                                            savedInFavorite = updatedFlat.savedInFavorites,
                                            disliked = updatedFlat.dislike,
                                            saveInFavoriteInProgress = false
                                        )
                                    } else uiFlat
                                }.toImmutableList()
                            )
                        }
                    }
                }
            }
    }

    private suspend fun PipeCtx.handleClearDislike(intent: FlatListIntent.ClearDislike) {
        mergedRepository.setFlatDisliked(intent.flatPlatform, intent.adId, disliked = false)
            .asLCE()
            .collect { lce ->
                if (lce is LCE.Content) {
                    val updatedFlat = lce.value
                    updateState {
                        copy(
                            flatList = flatList.map { uiFlat ->
                                if (uiFlat.adId == updatedFlat?.adId &&
                                    uiFlat.flatPlatform == updatedFlat.flatPlatform
                                ) {
                                    uiFlat.copy(disliked = updatedFlat.dislike)
                                } else uiFlat
                            }.toImmutableList()
                        )
                    }
                }
            }
    }

    private suspend fun PipeCtx.handleSetDisliked(intent: FlatListIntent.SetDisliked) {
        mergedRepository.setFlatDisliked(intent.flatPlatform, intent.adId, disliked = true)
            .asLCE()
            .collect { lce ->
                if (lce is LCE.Content) {
                    val updatedFlat = lce.value
                    updateState {
                        copy(
                            flatList = flatList.map { uiFlat ->
                                if (uiFlat.adId == updatedFlat?.adId &&
                                    uiFlat.flatPlatform == updatedFlat.flatPlatform
                                ) {
                                    uiFlat.copy(
                                        disliked = updatedFlat.dislike,
                                        savedInFavorite = updatedFlat.savedInFavorites,
                                    )
                                } else uiFlat
                            }.toImmutableList()
                        )
                    }
                }
            }
    }

    private suspend fun PipeCtx.handlePrefetchDetail(intent: FlatListIntent.PrefetchDetail) {
        mergedRepository.getFlatByIdWithDetails(
            flatPlatform = intent.flatPlatform,
            flatId = intent.adId,
            markAsViewed = intent.markAsViewed,
        ).asLCE().collect { /* DB flow updates list images/description */ }
    }

    private suspend fun PipeCtx.handleLoadDbFlats(intent: FlatListIntent.LoadDbFlats) {
        handleLoadDbFlatsFromFlow(intent.dbFlats)
    }

    private suspend fun PipeCtx.handleToggleView() {
        withState {
            val newIsListView = !isListView
            launch(Dispatchers.IO) {
                userPreferencesRepository.saveListViewPreferences(newIsListView)
            }
            updateState { copy(isListView = newIsListView) }
        }
    }

    private suspend fun PipeCtx.handleSetListView(intent: FlatListIntent.SetListView) {
        launch(Dispatchers.IO) {
            userPreferencesRepository.saveListViewPreferences(intent.isListView)
        }
        updateState { copy(isListView = intent.isListView) }
    }

    private suspend fun PipeCtx.loadAllFlats(
        isLoadMore: Boolean,
        isRefreshing: Boolean
    ) {
        // First page only: inflate high-volume sources so delay filter still leaves ads.
        filterRepository.listFetchBoostActive = !isLoadMore &&
                userTierProvider.currentTier() != UserTier.PREMIUM &&
                userTierProvider.feedDelayMinutes() > 0
        if (!connectionMonitor.isNetworkAvailable.first()) {
            mergedRepository.getAllFlatsFromLocalDb().asLCE().collect { lce ->
                when (lce) {
                    is LCE.Loading -> updateState {
                        copy(
                            isLoading = true,
                            isLoadingMore = isLoadMore,
                            isRefreshing = isRefreshing
                        )
                    }

                    is LCE.Error -> clearLoadingFlags()
                    is LCE.Content -> applyFlatsLoaded(
                        lce.value.filterForCurrentMarket(),
                        isLoadMore,
                        isRefreshing,
                    )
                }
            }
        } else {
            mergedRepository.searchFlats().asLCE().collect { lceResult ->
                when (lceResult) {
                    is LCE.Loading -> updateState {
                        copy(
                            isLoading = true,
                            isLoadingMore = isLoadMore,
                            isRefreshing = isRefreshing
                        )
                    }

                    is LCE.Error -> clearLoadingFlags()
                    is LCE.Content -> {
                        val response = lceResult.value
                        applyFlatsLoaded(response.flats, isLoadMore, isRefreshing)
                        // Soft-fail: show dialog when search yielded nothing, or a general hint
                        // (VPN on / try VPN for DNS failures). Sticky until the user dismisses —
                        // do not clear on later successful / partial results.
                        val shouldShowErrors = response.errors.hasDisplayableErrors &&
                                (response.flats.isEmpty() || response.errors.generalError != null)
                        if (shouldShowErrors) {
                            val fingerprint = SearchNetworkErrorDismissStore.fingerprint(
                                generalError = response.errors.generalError,
                                platformErrors = response.errors.platformErrors.map {
                                    it.platform to it.errorMessages
                                },
                            )
                            if (!SearchNetworkErrorDismissStore.isSuppressed(fingerprint)) {
                                updateState {
                                    copy(
                                        errorDialogState = SearchErrorDialogState(
                                            isVisible = true,
                                            dialogType = DialogType.NetworkError,
                                            title = LocalizationKeys.SEARCH_ERROR_TITLE,
                                            generalError = response.errors.generalError,
                                            searchedPlatforms = response.searchedPlatforms,
                                            fingerprint = fingerprint,
                                            errorInfo = response.errors.platformErrors.map {
                                                SearchErrorDialogState.ErrorInfo(
                                                    platform = it.platform,
                                                    errorMessages = it.errorMessages
                                                )
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun List<AppFlat>.filterForCurrentMarket(): List<AppFlat> {
        val country = filterRepository.lastFilter().location?.country ?: CountryCode.BY
        return filter { it.flatPlatform.marketCountry() == country }
    }

    private suspend fun PipeCtx.applyFlatsLoaded(
        flats: List<AppFlat>,
        isLoadMore: Boolean,
        isRefreshing: Boolean,
    ) {
        val filtered = flats.applyFeedDelayFilter(
            tier = userTierProvider.currentTier(),
            delayMinutes = userTierProvider.feedDelayMinutes(),
            publishedAt = { it.publishedAt },
        )
        val uiFlatList = UiFlat.appFlatListToUiFlatList(filtered)
        if (uiFlatList.isEmpty()) noFlatsToLoadMore = true

        if (isLoadMore) {
            var previous = emptyList<UiFlat>()
            withState { previous = flatList }
            val merged = (previous + uiFlatList)
                .distinctBy { it.flatPlatform to it.adId }
                .toImmutableList()
            // Client-side filters / duplicate pages → nothing new → stop paging.
            if (merged.size <= previous.size) noFlatsToLoadMore = true
            val endReached = noFlatsToLoadMore
            updateState {
                copy(
                    noFlatsToLoadMore = endReached,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = merged,
                    currentSearchPage = filterRepository.currentHomePage,
                )
            }
            return
        }

        if (uiFlatList.isNotEmpty()) noFlatsToLoadMore = false
        val endReached = noFlatsToLoadMore
        updateState {
            when {
                uiFlatList.isEmpty() -> copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    noFlatsToLoadMore = endReached,
                    currentSearchPage = filterRepository.currentHomePage,
                )

                else -> copy(
                    noFlatsToLoadMore = false,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    currentSearchPage = filterRepository.currentHomePage,
                )
            }
        }
    }
}
