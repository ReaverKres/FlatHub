package io.flatzen.viewmodel.list

import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.monetization.tier.applyFeedDelayFilter
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository
import repository.userpreferences.UserPreferencesRepository

private typealias PipeCtx = PipelineContext<FlatListScreenState, FlatListIntent, FlatListAction>

class FlatSearchContainer(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val connectionMonitor: ConnectionMonitor,
    private val analyticsManager: AnalyticsManager,
    private val configFieldsChecker: ConfigFieldsChecker,
    private val userTierProvider: UserTierProvider,
) : Container<FlatListScreenState, FlatListIntent, FlatListAction> {

    private var noFlatsToLoadMore: Boolean = false
    private var isNetworkAvailable: Boolean = true

    override val store = store(
        initial = FlatListScreenState.Initial
    ) {
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
            filterRepository.forceReloadFlow
                .collect {
                    loadAllFlatsFromFilterUpdate()
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

                is FlatListIntent.IsAnyFilterAppliedCheck -> {
                    updateState { copy(isAnyFilterApplied = intent.applied) }
                }

                is FlatListIntent.SearchFlats -> handleSearchFlats(intent)

                is FlatListIntent.ClickOnFavorite -> handleClickOnFavorite(intent)
                is FlatListIntent.ClearDislike -> handleClearDislike(intent)
                is FlatListIntent.SetDisliked -> handleSetDisliked(intent)
                is FlatListIntent.PrefetchDetail -> handlePrefetchDetail(intent)

                is FlatListIntent.LoadDbFlats -> handleLoadDbFlats(intent)

                is FlatListIntent.TrackScreenView -> {
                    launch(Dispatchers.IO) {
                        analyticsManager.registerEvent(
                            AnalyticsEvent(
                                eventName = AppMetrcica.Events.SCREEN_VIEW,
                                parameters = mapOf(
                                    AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                                    AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                                ) + intent.parameters
                            )
                        )
                    }
                }

                FlatListIntent.ToggleView -> handleToggleView()
                is FlatListIntent.SetListView -> handleSetListView(intent)
                FlatListIntent.HideNetworkErrorDialog -> {
                    updateState { copy(errorDialogState = null) }
                }
            }
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
        loadAllFlats(false, false)
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
                                        imageUrls = (flatFromDb.imageUrls ?: flatOnScreen.imageUrls).toImmutableList()
                                    )
                                } else flatOnScreen
                            }.toImmutableList()
                        )
                    }
                } else {
                    applyFlatsLoaded(dbFlats, false, false)
                }
            }
        }
    }

    private suspend fun PipeCtx.handleScreenVisible() {
        val lastNet = filterRepository.lastNetworkFilter
        val selectedFilter = filterRepository.getSelectedSavedFilter()
        if (selectedFilter != null && selectedFilter.filterData != lastNet) {
            filterRepository.applySavedFilter(selectedFilter, true)
        } else {
            if (filterRepository.cashedFilterFlow.replayCache.isEmpty()) {
                filterRepository.updateFilter(CommonFilterRequestModel(), true)
            } else {
                val last = filterRepository.lastFilter()
                if (lastNet != last) {
                    filterRepository.updateFilter(last, true)
                }
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
            analyticsManager.registerEvent(
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

        loadAllFlats(intent.isLoadMore, intent.isRefreshing)
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

                    is LCE.Error -> updateState { copy(isRefreshing = false) }
                    is LCE.Content -> applyFlatsLoaded(lce.value, isLoadMore, isRefreshing)
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

                    is LCE.Error -> updateState { copy(isRefreshing = false) }
                    is LCE.Content -> {
                        val response = lceResult.value
                        applyFlatsLoaded(response.flats, isLoadMore, isRefreshing)
                        if (response.errors.isNotEmpty() &&
                            response.errors.any { it.errorMessages.isNotEmpty() }
                        ) {
                            updateState {
                                copy(
                                    errorDialogState = SearchErrorDialogState(
                                        isVisible = true,
                                        dialogType = DialogType.NetworkError,
                                        title = LocalizationKeys.SEARCH_ERROR_TITLE,
                                        errorInfo = response.errors.map {
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

    private suspend fun PipeCtx.applyFlatsLoaded(
        flats: List<AppFlat>,
        isLoadMore: Boolean,
        isRefreshing: Boolean
    ) {
        val filtered = flats.applyFeedDelayFilter(
            tier = userTierProvider.currentTier(),
            delayMinutes = userTierProvider.feedDelayMinutes(),
            publishedAt = { it.publishedAt },
        )
        val uiFlatList = UiFlat.appFlatListToUiFlatList(filtered)
        if (uiFlatList.isEmpty()) noFlatsToLoadMore = true
        updateState {
            when {
                isLoadMore -> copy(
                    noFlatsToLoadMore = noFlatsToLoadMore,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = (flatList + uiFlatList).toImmutableList(),
                    currentSearchPage = filterRepository.currentHomePage
                )

                uiFlatList.isEmpty() -> copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    noFlatsToLoadMore = noFlatsToLoadMore,
                    currentSearchPage = filterRepository.currentHomePage
                )

                else -> copy(
                    noFlatsToLoadMore = false,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    currentSearchPage = filterRepository.currentHomePage
                )
            }
        }
    }
}
