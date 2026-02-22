package io.flatzen.viewmodel.list

import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
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
    private val configFieldsChecker: ConfigFieldsChecker
) : Container<FlatListScreenState, FlatListIntent, FlatListAction> {

    private var noFlatsToLoadMore: Boolean = false
    private var isNetworkAvailable: Boolean = true

    override val store = store<FlatListScreenState, FlatListIntent, FlatListAction>(
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
                        title = "Доступна новая версия",
                        description = "Текущая версия неработоспособна, пожалуйста обновите приложение"
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
                                        imageUrls = flatFromDb.imageUrls ?: flatOnScreen.imageUrls
                                    )
                                } else flatOnScreen
                            }
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
                        title = "Доступна новая версия",
                        description = "Текущая версия неработоспособна, пожалуйста обновите приложение"
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
                                    }
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
                                            saveInFavoriteInProgress = false
                                        )
                                    } else uiFlat
                                }
                            )
                        }
                    }
                }
            }
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
                                        title = "Произошла ошибка",
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
        val uiFlatList = UiFlat.appFlatListToUiFlatList(flats)
        if (isLoadMore && uiFlatList.isEmpty()) noFlatsToLoadMore = true
        if (uiFlatList.isEmpty() && !isLoadMore) noFlatsToLoadMore = true
        updateState {
            when {
                isLoadMore -> copy(
                    noFlatsToLoadMore = noFlatsToLoadMore,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = flatList + uiFlatList,
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
