package io.flatzen.viewmodel.notifications

import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.error_handling.LCE
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent

// Intent (was NotificationListScreenAction)
sealed interface NotificationListIntent : MVIIntent {
    data object ScrollToTop : NotificationListIntent
    data object ListViewCheck : NotificationListIntent
    data object IsNotificationPermissionGranted : NotificationListIntent
    data object ProvideNotifPermission : NotificationListIntent
    data object CloseNotifPermMessage : NotificationListIntent

    data class SearchFlats(
        val isLoadMore: Boolean,
        val isLoadMoreForce: Boolean = false,
        val isRefreshing: Boolean = false
    ) : NotificationListIntent

    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) :
        NotificationListIntent

    data class LoadDbFlats(val dbFlats: LCE<List<AppFlat>>) : NotificationListIntent

    data object ToggleView : NotificationListIntent
    data object HideNetworkErrorDialog : NotificationListIntent
    data object LoadSubscriptions : NotificationListIntent
    data class SelectSubscription(val id: String) : NotificationListIntent
    data class ShowParams(val id: String) : NotificationListIntent
    data object HideParams : NotificationListIntent
    data class DeleteSubscription(val id: String) : NotificationListIntent

    // Internal intents (not exposed to UI directly)
    data class SetListView(val isListView: Boolean) : NotificationListIntent
    data class DeleteSubscriptionFailed(val id: String, val message: String) :
        NotificationListIntent

    data class OpenDetail(val flatPlatform: FlatPlatform, val adId: Long) : NotificationListIntent
    data object NavigateBack : NotificationListIntent
}

// Action (was NotificationListEffect)
sealed interface NotificationListAction : MVIAction {
    data object ScrollToTopEffect : NotificationListAction
    data class NotifPermGrantedEffect(val isGranted: Boolean) : NotificationListAction
    data object ShowSettingsEffect : NotificationListAction
}
