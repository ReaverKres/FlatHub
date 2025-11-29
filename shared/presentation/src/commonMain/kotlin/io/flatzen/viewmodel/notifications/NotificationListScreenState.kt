package io.flatzen.viewmodel.notifications

import androidx.compose.runtime.Immutable
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.list.UiFlat
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState

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
) : MviState
