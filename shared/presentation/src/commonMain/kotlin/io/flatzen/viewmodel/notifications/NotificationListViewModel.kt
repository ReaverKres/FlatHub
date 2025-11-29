package io.flatzen.viewmodel.notifications

import core.NetworkErrorInfo
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.list.FlatListEvents.ScrollToTopEvent
import io.flatzen.viewmodel.sharedstates.DialogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import repository.fillter.FilterRepository
import repository.mergedrepo.MergedRepository
import repository.userpreferences.UserPreferencesRepository

sealed interface NotificationListScreenAction : MviAction {
    data object ScrollToTop : NotificationListScreenAction
    class SearchFlats(
        val isLoadMore: Boolean,
        val isLoadMoreForce: Boolean = false,
        val isRefreshing: Boolean = false
    ) :
        NotificationListScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : NotificationListScreenAction

    //    class LoadFavorites(val favoritesFlats: LCE<List<AppFlat>>) : FlatListScreenAction
    class LoadDbFlats(val dbFlats: LCE<List<AppFlat>>) : NotificationListScreenAction
    data object ScreenVisible : NotificationListScreenAction

    // View toggle actions
    data object ToggleView : NotificationListScreenAction
    class SetListView(val isListView: Boolean) : NotificationListScreenAction
    object HideNetworkErrorDialog : NotificationListScreenAction
}

sealed interface NotificationListEvents : MviEvent {
    data object ScrollToTopEvent : NotificationListEvents
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
    class IsAnyFilterApplied(val applied: Boolean) : NotificationListEvents
    class ViewToggled(val isListView: Boolean) : NotificationListEvents
    class InfoDialogShowed(val dialogType: DialogType, val title: String, val description: String) :
        NotificationListEvents

    class ErrorDialogShowed(
        val dialogType: DialogType,
        val title: String,
        val networkErrorInfo: List<NetworkErrorInfo>
    ) : NotificationListEvents

    class ErrorDialogHidden() : NotificationListEvents
}

sealed interface NotificationListEffect : MviEffect {
    data object ScrollToTopEffect : NotificationListEffect
}

class NotificationListViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseMviViewModel<NotificationListScreenAction, NotificationListScreenState, NotificationListEvents, NotificationListEffect>() {

    private var noFlatsToLoadMore: Boolean = false

    init {

        mergedRepository.getAllFlatsFromLocalDb()
            .asLCE()
            .onEach { event -> onIntent(NotificationListScreenAction.LoadDbFlats(event)) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    override fun initialState(): NotificationListScreenState = NotificationListScreenState(
        isLoading = true,
        isRefreshing = false,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false,
        currentSearchPage = 1
    )

    override suspend fun handleIntent(
        action: NotificationListScreenAction,
        currentState: NotificationListScreenState
    ): Flow<NotificationListEvents> {
        TODO("Not yet implemented")
    }

    override suspend fun reduce(
        event: NotificationListEvents,
        currentState: NotificationListScreenState
    ): NotificationListScreenState {
        TODO("Not yet implemented")
    }

    override suspend fun onEvent(event: NotificationListEvents): NotificationListEffect? {
        return when (event) {
            ScrollToTopEvent -> NotificationListEffect.ScrollToTopEffect
            else -> super.onEvent(event)
        }
    }
}