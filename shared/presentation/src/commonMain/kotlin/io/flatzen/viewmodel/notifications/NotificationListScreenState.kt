package io.flatzen.viewmodel.notifications

import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.filter.mapFilterModelToFilterState
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import pro.respawn.flowmvi.api.MVIState

@Immutable
data class NotificationListScreenState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val noFlatsToLoadMore: Boolean,
    val flatList: List<UiFlat>,
    val currentSearchPage: Int,
    val isListView: Boolean = false,
    val errorDialogState: SearchErrorDialogState? = null,
    val subscriptions: List<SubscriptionUi> = emptyList(),
    val paramsDialogText: String? = null,
    val errorText: String? = null
) : MviState, MVIState {
    companion object {
        fun initial(currentSearchPage: Int = 1) = NotificationListScreenState(
            isLoading = true,
            isRefreshing = false,
            isLoadingMore = false,
            flatList = emptyList(),
            noFlatsToLoadMore = false,
            currentSearchPage = currentSearchPage,
            subscriptions = emptyList(),
            paramsDialogText = null,
            errorText = null
        )
    }
}

@Immutable
data class SubscriptionUi(
    val id: String,
    val name: String?,
    internal val filter: CommonFilterRequestModel,
    val selected: Boolean,
    val ordinal: Int
) {
    val filterUi = mapFilterModelToFilterState(filter)
}
