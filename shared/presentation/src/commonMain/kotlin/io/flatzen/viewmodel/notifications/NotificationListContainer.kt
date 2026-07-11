package io.flatzen.viewmodel.notifications

import api.toModel
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.viewmodel.filter.mapFilterModelToFilterState
import io.flatzen.viewmodel.list.UiFlat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.mergedrepo.MergedRepository
import repository.subscriptions.SubscriptionsRepository
import repository.userpreferences.UserPreferencesRepository

object NotifPermissionMessageVisibility {
    var isShown: Boolean = false
}

private fun getActiveFiltersText(model: CommonFilterRequestModel): String {
    val filterState = mapFilterModelToFilterState(model)
    return filterState.getActiveFiltersText()
}

private typealias PipeCtx = PipelineContext<NotificationListScreenState, NotificationListIntent, NotificationListAction>

class NotificationListContainer(
    private val mergedRepository: MergedRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val permissionsController: PermissionsController,
    private val devicePlatform: DevicePlatform,
    private val filterFromNotification: String?
) : Container<NotificationListScreenState, NotificationListIntent, NotificationListAction> {

    private var noFlatsToLoadMore: Boolean = false
    private var selectedFilter: CommonFilterRequestModel? = null
    private var currentPage: Int = 1
    private val pendingDeletedSubscriptions = mutableMapOf<String, SubscriptionUi>()

    override val store =
        store<NotificationListScreenState, NotificationListIntent, NotificationListAction>(
            initial = NotificationListScreenState.initial(1)
        ) {
            whileSubscribed {
                mergedRepository.getAllFlatsFromLocalDb()
                    .asLCE()
                    .onEach { lce -> intent(NotificationListIntent.LoadDbFlats(lce)) }
                    .flowOn(Dispatchers.IO)
                    .collect { }
            }

            reduce { intent ->
                when (intent) {
                    NotificationListIntent.IsNotificationPermissionGranted -> {
                        if (!NotifPermissionMessageVisibility.isShown) {
                            val isGranted =
                                permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                            action(NotificationListAction.NotifPermGrantedEffect(isGranted))
                        }
                    }

                    NotificationListIntent.ProvideNotifPermission -> {
                        try {
                            permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                        } catch (_: DeniedAlwaysException) {
                            action(NotificationListAction.ShowSettingsEffect)
                        } catch (_: DeniedException) {
                        }
                    }

                    NotificationListIntent.CloseNotifPermMessage -> {
                        NotifPermissionMessageVisibility.isShown = true
                    }

                    NotificationListIntent.ListViewCheck -> {
                        userPreferencesRepository.getUserPreferences().firstOrNull()
                            ?.let { preferences ->
                                updateState { copy(isListView = preferences.isListView) }
                            }
                    }

                    is NotificationListIntent.ClickOnFavorite -> handleClickOnFavorite(intent)

                    is NotificationListIntent.DeleteSubscription -> handleDeleteSubscription(intent)

                    is NotificationListIntent.DeleteSubscriptionFailed -> handleDeleteSubscriptionFailed(
                        intent
                    )

                    NotificationListIntent.HideNetworkErrorDialog -> {
                        updateState { copy(errorText = null) }
                    }

                    NotificationListIntent.HideParams -> {
                        updateState { copy(paramsDialogText = null) }
                    }

                    is NotificationListIntent.LoadDbFlats -> handleLoadDbFlats(intent)

                    NotificationListIntent.LoadSubscriptions -> loadSubscriptions()

                    NotificationListIntent.ScrollToTop -> action(NotificationListAction.ScrollToTopEffect)

                    is NotificationListIntent.SearchFlats -> handleSearchFlats(intent)

                    is NotificationListIntent.SelectSubscription -> handleSelectSubscription(intent)

                    is NotificationListIntent.SetListView -> {
                        launch(Dispatchers.IO) {
                            userPreferencesRepository.saveListViewPreferences(intent.isListView)
                        }
                        updateState { copy(isListView = intent.isListView) }
                    }

                    is NotificationListIntent.ShowParams -> {
                        withState {
                            val sub = subscriptions.find { it.id == intent.id }
                            val text = sub?.let { getActiveFiltersText(it.filter) }
                            updateState { copy(paramsDialogText = text) }
                        }
                    }

                    NotificationListIntent.ToggleView -> {
                        withState {
                            val newIsListView = !isListView
                            launch(Dispatchers.IO) {
                                userPreferencesRepository.saveListViewPreferences(newIsListView)
                            }
                            updateState { copy(isListView = newIsListView) }
                        }
                    }
                }
            }
        }

    private suspend fun PipeCtx.handleClickOnFavorite(intent: NotificationListIntent.ClickOnFavorite) {
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

    private suspend fun PipeCtx.handleDeleteSubscription(intent: NotificationListIntent.DeleteSubscription) {
        withState {
            val sub = subscriptions.find { it.id == intent.id }
            sub?.let { pendingDeletedSubscriptions[intent.id] = it }
            val newList = subscriptions.filter { it.id != intent.id }
            updateState { copy(subscriptions = newList.toImmutableList()) }

            launch(Dispatchers.IO) {
                try {
                    subscriptionsRepository.deleteById(intent.id)
                    pendingDeletedSubscriptions.remove(intent.id)
                } catch (e: Exception) {
                    store.intent(
                        NotificationListIntent.DeleteSubscriptionFailed(
                            intent.id,
                            e.cause?.message ?: "Не удалось удалить подписку"
                        )
                    )
                }
            }

            val firstSub = newList.firstOrNull()
            if (firstSub != null) {
                store.intent(NotificationListIntent.SelectSubscription(firstSub.id))
            } else {
                selectedFilter = null
                updateState {
                    copy(
                        isLoading = false,
                        flatList = persistentListOf(),
                        noFlatsToLoadMore = false
                    )
                }
            }
        }
    }

    private suspend fun PipeCtx.handleDeleteSubscriptionFailed(intent: NotificationListIntent.DeleteSubscriptionFailed) {
        val restoredItem = pendingDeletedSubscriptions.remove(intent.id)
        withState {
            val restored = if (restoredItem != null) {
                subscriptions + restoredItem
            } else {
                subscriptions
            }
            updateState { copy(subscriptions = restored.toImmutableList(), errorText = intent.message) }
        }
    }

    private suspend fun PipeCtx.handleLoadDbFlats(intent: NotificationListIntent.LoadDbFlats) {
        when (intent.dbFlats) {
            is LCE.Loading -> {}
            is LCE.Error -> {}
            is LCE.Content -> {
                val dbFlats = intent.dbFlats.value
                updateState {
                    copy(
                        flatList = flatList.map { uiFlat ->
                            val flatFromDb = dbFlats.find { f ->
                                f.adId == uiFlat.adId && f.flatPlatform == uiFlat.flatPlatform
                            }
                            if (flatFromDb != null) {
                                uiFlat.copy(
                                    savedInFavorite = flatFromDb.savedInFavorites,
                                    isViewed = flatFromDb.isViewed,
                                    imageUrls = (flatFromDb.imageUrls ?: uiFlat.imageUrls).toImmutableList()
                                )
                            } else uiFlat
                        }.toImmutableList()
                    )
                }
            }
        }
    }

    private suspend fun PipeCtx.loadSubscriptions() {
        subscriptionsRepository.listByDevice(devicePlatform.deviceId).asLCE()
            .collect { lce ->
                when (lce) {
                    is LCE.Loading -> updateState { copy(isLoading = true) }
                    is LCE.Error -> updateState {
                        copy(
                            isRefreshing = false,
                            isLoadingMore = false,
                            isLoading = false,
                            errorText = lce.throwable?.cause?.message
                        )
                    }

                    is LCE.Content -> {
                        val list = lce.value
                        val uiSubs = list.mapIndexed { idx, it ->
                            SubscriptionUi(
                                id = it.id,
                                name = it.name,
                                filter = it.filter.toModel(),
                                selected = idx == 0,
                                ordinal = idx + 1
                            )
                        }
                        updateState {
                            copy(
                                isRefreshing = false,
                                isLoadingMore = false,
                                isLoading = uiSubs.isNotEmpty(),
                                subscriptions = uiSubs.toImmutableList()
                            )
                        }
                        if (uiSubs.isNotEmpty()) {
                            store.intent(NotificationListIntent.SelectSubscription(uiSubs.first().id))
                        }
                    }
                }
            }
    }

    private suspend fun PipeCtx.handleSelectSubscription(intent: NotificationListIntent.SelectSubscription) {
        withState {
            val sub = subscriptions.find { it.id == intent.id }
            sub?.let {
                selectedFilter = it.filter
                updateState {
                    copy(
                        subscriptions = subscriptions.map { s -> s.copy(selected = s.id == intent.id) }
                            .toImmutableList()
                    )
                }
                store.intent(
                    NotificationListIntent.SearchFlats(
                        isLoadMore = false,
                        isRefreshing = false
                    )
                )
            }
        }
    }

    private suspend fun PipeCtx.handleSearchFlats(intent: NotificationListIntent.SearchFlats) {
        if (!intent.isLoadMoreForce && noFlatsToLoadMore && !intent.isRefreshing) return

        val filter = selectedFilter ?: return

        if (intent.isRefreshing || !intent.isLoadMore) {
            currentPage = 1
            mergedRepository.clearCashedFlats()
        }
        if (intent.isLoadMore) {
            currentPage++
        }
        if (!intent.isLoadMore) {
            action(NotificationListAction.ScrollToTopEffect)
        }

        mergedRepository.searchFlats(filter, currentPage).asLCE()
            .collect { lceResult ->
                when (lceResult) {
                    is LCE.Loading -> updateState {
                        copy(
                            isLoading = true,
                            isLoadingMore = intent.isLoadMore,
                            isRefreshing = intent.isRefreshing
                        )
                    }

                    is LCE.Error -> updateState {
                        copy(
                            isRefreshing = false,
                            isLoadingMore = false,
                            isLoading = false
                        )
                    }

                    is LCE.Content -> {
                        val response = lceResult.value
                        val uiFlatList = UiFlat.appFlatListToUiFlatList(response.flats)
                        if (intent.isLoadMore && uiFlatList.isEmpty()) noFlatsToLoadMore = true
                        if (uiFlatList.isEmpty() && !intent.isLoadMore) noFlatsToLoadMore = true

                        updateState {
                            when {
                                intent.isLoadMore -> copy(
                                    noFlatsToLoadMore = noFlatsToLoadMore,
                                    isRefreshing = false,
                                    isLoading = false,
                                    isLoadingMore = false,
                                    flatList = (flatList + uiFlatList).toImmutableList(),
                                    currentSearchPage = currentPage
                                )

                                uiFlatList.isEmpty() -> copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isLoadingMore = false,
                                    flatList = uiFlatList,
                                    noFlatsToLoadMore = noFlatsToLoadMore,
                                    currentSearchPage = currentPage
                                )

                                else -> copy(
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
                }
            }
    }
}
