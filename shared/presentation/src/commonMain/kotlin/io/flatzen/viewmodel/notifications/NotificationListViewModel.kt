package io.flatzen.viewmodel.notifications

import api.SubscriptionDocument
import api.toModel
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.mapFilterModelToFilterState
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.notifications.NotificationListScreenAction.ScrollToTop
import io.flatzen.viewmodel.notifications.NotificationListScreenAction.SearchFlats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import repository.mergedrepo.MergedRepository
import repository.subscriptions.SubscriptionsRepository
import repository.userpreferences.UserPreferencesRepository

object NotifPermissionMessageVisibility{
    var isShown: Boolean = false
}

sealed interface NotificationListScreenAction : MviAction {
    data object ScrollToTop : NotificationListScreenAction
    data object ListViewCheck : NotificationListScreenAction
    data object IsNotificationPermissionGranted : NotificationListScreenAction
    data object ProvideNotifPermission : NotificationListScreenAction
    data object CloseNotifPermMessage: NotificationListScreenAction

    class SearchFlats(
        val isLoadMore: Boolean,
        val isLoadMoreForce: Boolean = false,
        val isRefreshing: Boolean = false
    ) :
        NotificationListScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) :
        NotificationListScreenAction

    class LoadDbFlats(val dbFlats: LCE<List<AppFlat>>) : NotificationListScreenAction

    // View toggle actions
    data object ToggleView : NotificationListScreenAction
    object HideNetworkErrorDialog : NotificationListScreenAction
    data object LoadSubscriptions : NotificationListScreenAction
    data class SelectSubscription(val id: String) : NotificationListScreenAction
    data class ShowParams(val id: String) : NotificationListScreenAction
    data object HideParams : NotificationListScreenAction
    data class DeleteSubscription(val id: String) : NotificationListScreenAction
}

private sealed interface PrivateNotificationAction : NotificationListScreenAction {
    class SetListView(val isListView: Boolean) : PrivateNotificationAction
    data class DeleteSubscriptionFailed(val id: String, val message: String) :
        PrivateNotificationAction
}

sealed interface NotificationListEvents : MviEvent {
    data object SubsIsEmptyEvent : NotificationListEvents
    data object ScrollToTopNotifEvent : NotificationListEvents
    data object NotifPermGrantedEvent : NotificationListEvents
    data object ShowSettingsEvent : NotificationListEvents
    data class AllFlatsLoaded(
        val allFlats: LCE<List<AppFlat>>,
        val isLoadMore: Boolean,
        val isRefreshing: Boolean
    ) :
        NotificationListEvents

    data class FlatUpdateInFavorite(
        val flat: LCE<AppFlat?>,
        val flatPlatform: FlatPlatform,
        val adId: Long
    ) : NotificationListEvents

    data class DbFlatsLoaded(val dbFlats: LCE<List<AppFlat>>) : NotificationListEvents
    class NotifViewToggled(val isListView: Boolean) : NotificationListEvents

    class ErrorDialogHidden() : NotificationListEvents
    data class SubscriptionsLoaded(val list: LCE<List<SubscriptionDocument>>) :
        NotificationListEvents

    data class SubscriptionSelected(val id: String, val filter: CommonFilterRequestModel) :
        NotificationListEvents

    data class ParamsDialogUpdated(val text: String?) : NotificationListEvents
    data class SubscriptionsUiUpdated(val subs: List<SubscriptionUi>) : NotificationListEvents
    data class DeleteErrorShown(val message: String) : NotificationListEvents
}

sealed interface NotificationListEffect : MviEffect {
    data object ScrollToTopEffect : NotificationListEffect
    data class NotifPermGrantedEffect(val isGranted: Boolean) : NotificationListEffect
    data object ShowSettingsEffect : NotificationListEffect
}

class NotificationListViewModel(
    private val mergedRepository: MergedRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val permissionsController: PermissionsController,
    private val devicePlatform: DevicePlatform,
    private val filterFromNotification: String?
) : BaseMviViewModel<NotificationListScreenAction, NotificationListScreenState, NotificationListEvents, NotificationListEffect>() {

    private var noFlatsToLoadMore: Boolean = false
    private var selectedFilter: CommonFilterRequestModel? = null
    private var currentPage: Int = 1
    private val pendingDeletedSubscriptions: MutableMap<String, SubscriptionUi> = mutableMapOf()
    private val subscriptions: MutableList<SubscriptionUi> = mutableListOf()

    init {

        state.onEach {
            subscriptions.clear()
            subscriptions.addAll(it.subscriptions)
        }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        mergedRepository.getAllFlatsFromLocalDb()
            .asLCE()
            .onEach { event -> onIntent(NotificationListScreenAction.LoadDbFlats(event)) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)

        onIntent(NotificationListScreenAction.LoadSubscriptions)
    }

    override fun initialState(): NotificationListScreenState {
        currentPage = 1
        return NotificationListScreenState(
            isLoading = true,
            isRefreshing = false,
            isLoadingMore = false,
            flatList = emptyList(),
            noFlatsToLoadMore = false,
            currentSearchPage = currentPage,
            subscriptions = emptyList(),
            paramsDialogText = null,
            errorText = null
        )
    }

    override suspend fun handleIntent(
        action: NotificationListScreenAction,
        currentState: NotificationListScreenState
    ): Flow<NotificationListEvents> {
        return when (action) {
            is NotificationListScreenAction.IsNotificationPermissionGranted -> {
                if(NotifPermissionMessageVisibility.isShown.not()) {
                    flowOf(NotificationListEvents.NotifPermGrantedEvent)
                } else {
                    flowOf()
                }
            }

            is NotificationListScreenAction.ProvideNotifPermission -> {
                var event: Flow<NotificationListEvents> = flowOf()
                try {
                    permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                } catch (_: DeniedAlwaysException) {
                    event = flowOf(NotificationListEvents.ShowSettingsEvent)
                } catch (_: DeniedException) { }
                event
            }

            is NotificationListScreenAction.CloseNotifPermMessage -> {
                NotifPermissionMessageVisibility.isShown = true
                flowOf()
            }

            is NotificationListScreenAction.ListViewCheck -> {
                userPreferencesRepository.getUserPreferences().firstOrNull()?.let { preferences ->
                    flowOf(NotificationListEvents.NotifViewToggled(preferences.isListView))
                } ?: flowOf()
            }

            is NotificationListScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId)
                    .asLCE()
                    .map {
                        NotificationListEvents.FlatUpdateInFavorite(
                            it,
                            action.flatPlatform,
                            action.adId
                        )
                    }
            }

            is NotificationListScreenAction.DeleteSubscription -> {
                val events = mutableListOf<NotificationListEvents>()
                // Optimistic update: remove locally and attempt delete
                currentState.subscriptions.find { it.id == action.id }?.let { sub ->
                    pendingDeletedSubscriptions[action.id] = sub
                }
                val newList = currentState.subscriptions.filter { it.id != action.id }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        subscriptionsRepository.deleteById(action.id)
                        // Success: remove from pending store
                        pendingDeletedSubscriptions.remove(action.id)
                    } catch (e: Exception) {
                        onIntent(
                            PrivateNotificationAction.DeleteSubscriptionFailed(
                                action.id,
                                e.cause?.message ?: "Не удалось удалить подписку"
                            )
                        )
                    }
                }
                events.add(NotificationListEvents.SubscriptionsUiUpdated(newList))
                val firstSub = newList.firstOrNull()
                if (firstSub != null) {
                    onIntent(NotificationListScreenAction.SelectSubscription(firstSub.id))
                } else {
                    selectedFilter = null
                    events.add(
                        NotificationListEvents.SubsIsEmptyEvent
                    )
                }
                flowOf(*events.toTypedArray())
            }

            is PrivateNotificationAction.DeleteSubscriptionFailed -> {
                // Restore from local pending store and show error
                val restoredItem = pendingDeletedSubscriptions.remove(action.id)
                val restored = if (restoredItem != null) {
                    currentState.subscriptions + restoredItem
                } else {
                    currentState.subscriptions
                }
                flowOf(
                    NotificationListEvents.SubscriptionsUiUpdated(restored),
                    NotificationListEvents.DeleteErrorShown(action.message)
                )
            }

            NotificationListScreenAction.HideNetworkErrorDialog -> {
                flowOf(NotificationListEvents.ErrorDialogHidden())
            }

            NotificationListScreenAction.HideParams -> {
                flowOf(NotificationListEvents.ParamsDialogUpdated(null))
            }

            is NotificationListScreenAction.LoadDbFlats -> {
                flowOf(NotificationListEvents.DbFlatsLoaded(action.dbFlats))
            }

            NotificationListScreenAction.LoadSubscriptions -> {
                loadSubscription()
            }

            ScrollToTop -> {
                flowOf(NotificationListEvents.ScrollToTopNotifEvent)
            }

            is SearchFlats -> {
                if (action.isLoadMoreForce.not() && noFlatsToLoadMore && action.isRefreshing.not()) {
                    return flowOf()
                }
                if (action.isRefreshing || action.isLoadMore.not()) {
                    currentPage = 1
                    mergedRepository.clearCashedFlats()
                }
                if (action.isLoadMore) {
                    currentPage++
                }
                if (action.isLoadMore.not()) {
                    onIntent(ScrollToTop)
                }
                loadFlatsWithSelectedFilter(action.isLoadMore, action.isRefreshing)
            }

            is NotificationListScreenAction.SelectSubscription -> {
                val sub = subscriptions.find { it.id == action.id }
                sub?.let {
                    flowOf(NotificationListEvents.SubscriptionSelected(it.id, it.filter))
                } ?: flowOf()
            }

            is PrivateNotificationAction.SetListView -> {
                viewModelScope.launch(Dispatchers.IO) {
                    userPreferencesRepository.saveListViewPreferences(action.isListView)
                }
                flowOf(NotificationListEvents.NotifViewToggled(action.isListView))
            }

            is NotificationListScreenAction.ShowParams -> {
                val sub = currentState.subscriptions.find { it.id == action.id }
                val text = sub?.let { getActiveFiltersText(it.filter) }
                flowOf(NotificationListEvents.ParamsDialogUpdated(text))
            }

            NotificationListScreenAction.ToggleView -> {
                val newIsListView = !currentState.isListView
                viewModelScope.launch(Dispatchers.IO) {
                    userPreferencesRepository.saveListViewPreferences(newIsListView)
                }
                flowOf(NotificationListEvents.NotifViewToggled(newIsListView))
            }
        }
    }

    private fun loadSubscription(): Flow<NotificationListEvents.SubscriptionsLoaded> {
        return subscriptionsRepository.listByDevice(devicePlatform.deviceId).asLCE().map {
            NotificationListEvents.SubscriptionsLoaded(it)
        }
    }

    private suspend fun loadFlatsWithSelectedFilter(
        isLoadMore: Boolean,
        isRefreshing: Boolean
    ): Flow<NotificationListEvents> {
        val filter = selectedFilter ?: return flowOf()
        return mergedRepository.searchFlats(filter, currentPage)
            .asLCE()
            .flatMapConcat { lceResult ->
                when (lceResult) {
                    is LCE.Content -> {
                        val response = lceResult.value
                        val flatsLce = LCE.Content(response.flats) as LCE<List<AppFlat>>
                        flowOf(
                            NotificationListEvents.AllFlatsLoaded(
                                flatsLce,
                                isLoadMore,
                                isRefreshing
                            )
                        )
                    }

                    is LCE.Error -> {
                        val errorLce =
                            LCE.Error<List<AppFlat>>(lceResult.message, lceResult.throwable)
                        flowOf(
                            NotificationListEvents.AllFlatsLoaded(
                                errorLce,
                                isLoadMore,
                                isRefreshing
                            )
                        )
                    }

                    is LCE.Loading -> {
                        val loadingLce = LCE.Loading<List<AppFlat>>()
                        flowOf(
                            NotificationListEvents.AllFlatsLoaded(
                                loadingLce,
                                isLoadMore,
                                isRefreshing
                            )
                        )
                    }
                }
            }
    }

    override suspend fun reduce(
        event: NotificationListEvents,
        currentState: NotificationListScreenState
    ): NotificationListScreenState {
        return when (event) {
            is NotificationListEvents.SubsIsEmptyEvent -> {
                initialState().copy(isLoading = false)
            }

            is NotificationListEvents.AllFlatsLoaded -> event.allFlats.process(
                onLoading = {
                    currentState.copy(
                        isLoading = true,
                        isLoadingMore = event.isLoadMore,
                        isRefreshing = event.isRefreshing
                    )
                },
                onError = { _, _ ->
                    currentState.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        isLoading = false
                    )
                },
                onSuccess = {
                    val uiFlatList = UiFlat.appFlatListToUiFlatList(it)
                    when {
                        event.isLoadMore -> {
                            if (uiFlatList.isEmpty()) noFlatsToLoadMore = true
                            currentState.copy(
                                noFlatsToLoadMore = noFlatsToLoadMore,
                                isRefreshing = false,
                                isLoading = false,
                                isLoadingMore = false,
                                flatList = currentState.flatList + uiFlatList,
                                currentSearchPage = currentPage
                            )
                        }

                        uiFlatList.isEmpty() -> {
                            noFlatsToLoadMore = true
                            currentState.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isLoadingMore = false,
                                flatList = uiFlatList,
                                noFlatsToLoadMore = noFlatsToLoadMore,
                                currentSearchPage = currentPage
                            )
                        }

                        else -> {
                            currentState.copy(
                                noFlatsToLoadMore = false,
                                isRefreshing = false,
                                isLoading = false,
                                isLoadingMore = false,
                                flatList = uiFlatList,
                                currentSearchPage = currentPage
                            )
                        }
                    }
                }
            )

            is NotificationListEvents.DbFlatsLoaded -> event.dbFlats.process(
                onLoading = { currentState },
                onError = { _, _ -> currentState },
                onSuccess = { dbFlats ->
                    currentState.copy(
                        flatList = currentState.flatList.map { uiFlat ->
                            val flatFromDb = dbFlats.find { it.adId == uiFlat.adId }
                            if (flatFromDb != null) {
                                uiFlat.copy(
                                    savedInFavorite = flatFromDb.savedInFavorites,
                                    isViewed = flatFromDb.isViewed,
                                    imageUrls = flatFromDb.imageUrls ?: uiFlat.imageUrls
                                )
                            } else uiFlat
                        }
                    )
                }
            )

            is NotificationListEvents.ScrollToTopNotifEvent -> currentState
            is NotificationListEvents.NotifPermGrantedEvent -> currentState
            is NotificationListEvents.ShowSettingsEvent -> currentState

            is NotificationListEvents.SubscriptionsLoaded -> {
                event.list.process(
                    onSuccess = { list ->
                        val uiSubs = list.mapIndexed { idx, it ->
                            SubscriptionUi(
                                id = it.id,
                                name = it.name,
                                filter = it.filter.toModel(),
                                selected = idx == 0,
                                ordinal = idx + 1
                            )
                        }
                        if (uiSubs.isNotEmpty()) {
                            subscriptions.clear()
                            subscriptions.addAll(uiSubs)

                            onIntent(NotificationListScreenAction.SelectSubscription(uiSubs.first().id))
                        }
                        currentState.copy(
                            isRefreshing = false,
                            isLoadingMore = false,
                            isLoading = uiSubs.isNotEmpty(),
                            subscriptions = uiSubs
                        )
                    },
                    onError = { message, e ->
                        currentState.copy(
                            isRefreshing = false,
                            isLoadingMore = false,
                            isLoading = false,
                            errorText = e.cause?.message
                        )
                    },
                    onLoading = {
                        currentState.copy(isLoading = true)
                    }
                )
            }

            is NotificationListEvents.SubscriptionsUiUpdated -> {
                currentState.copy(subscriptions = event.subs)
            }

            is NotificationListEvents.SubscriptionSelected -> {
                selectedFilter = event.filter
                currentState.copy(
                    subscriptions = currentState.subscriptions.map { it.copy(selected = it.id == event.id) },
                ).also {
                    onIntent(SearchFlats(isLoadMore = false, isRefreshing = false))
                }
            }

            is NotificationListEvents.ParamsDialogUpdated -> {
                currentState.copy(paramsDialogText = event.text)
            }

            is NotificationListEvents.DeleteErrorShown -> {
                currentState.copy(errorText = event.message)
            }

            is NotificationListEvents.ErrorDialogHidden -> {
                currentState.copy(errorText = null)
            }

            is NotificationListEvents.FlatUpdateInFavorite -> {
                val detailFlatAccessible = currentState.flatList.find {
                    it.adId == event.adId && it.flatPlatform == event.flatPlatform
                }?.isDetailDataLoaded == true
                event.flat.process(
                    onLoading = {
                        if (detailFlatAccessible) {
                            currentState
                        } else {
                            val updatedList = currentState.flatList.map { uiFlat ->
                                if (uiFlat.adId == event.adId && event.flatPlatform == uiFlat.flatPlatform) {
                                    uiFlat.copy(
                                        saveInFavoriteInProgress = true
                                    )
                                } else {
                                    uiFlat
                                }
                            }
                            currentState.copy(flatList = updatedList)
                        }
                    },
                    onError = { _, _ -> currentState },
                    onSuccess = { updatedFlat ->
                        val updatedList = currentState.flatList.map { uiFlat ->
                            if (uiFlat.adId == updatedFlat?.adId && uiFlat.flatPlatform == updatedFlat.flatPlatform) {
                                uiFlat.copy(
                                    savedInFavorite = updatedFlat.savedInFavorites,
                                    saveInFavoriteInProgress = false
                                )
                            } else {
                                uiFlat
                            }
                        }
                        currentState.copy(flatList = updatedList)
                    }
                )
            }

            is NotificationListEvents.NotifViewToggled -> {
                currentState.copy(isListView = event.isListView)
            }
        }
    }

    override suspend fun onEvent(event: NotificationListEvents): NotificationListEffect? {
        return when (event) {
            NotificationListEvents.ScrollToTopNotifEvent -> NotificationListEffect.ScrollToTopEffect
            NotificationListEvents.NotifPermGrantedEvent -> {
                NotificationListEffect.NotifPermGrantedEffect(
                    permissionsController.isPermissionGranted(
                        Permission.REMOTE_NOTIFICATION
                    )
                )
            }

            NotificationListEvents.ShowSettingsEvent -> NotificationListEffect.ShowSettingsEffect

            else -> super.onEvent(event)
        }
    }
}

data class SubscriptionUi(
    val id: String,
    val name: String?,
    internal val filter: CommonFilterRequestModel,
    val selected: Boolean,
    val ordinal: Int
) {
    val filterUi = mapFilterModelToFilterState(filter)
}

private fun getActiveFiltersText(model: CommonFilterRequestModel): String {
    val filterState = mapFilterModelToFilterState(model)
    return filterState.getActiveFiltersText()
}