package io.flatzen.viewmodel.list

import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent

// Intent (was FlatListScreenAction)
sealed interface FlatListIntent : MVIIntent {
    data object ScrollToTop : FlatListIntent
    data class SearchFlats(
        val isLoadMore: Boolean,
        val isLoadMoreForce: Boolean = false,
        val isRefreshing: Boolean = false
    ) : FlatListIntent

    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatListIntent
    data class ClearDislike(val flatPlatform: FlatPlatform, val adId: Long) : FlatListIntent
    data class SetDisliked(val flatPlatform: FlatPlatform, val adId: Long) : FlatListIntent
    data class PrefetchDetail(
        val flatPlatform: FlatPlatform,
        val adId: Long,
        val markAsViewed: Boolean = true,
    ) : FlatListIntent
    data class LoadDbFlats(val dbFlats: io.flatzen.error_handling.LCE<List<AppFlat>>) :
        FlatListIntent

    data class IsAnyFilterAppliedCheck(val applied: Boolean) : FlatListIntent
    data object ScreenVisible : FlatListIntent

    data class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatListIntent

    data object ToggleView : FlatListIntent
    data class SetListView(val isListView: Boolean) : FlatListIntent
    data object HideNetworkErrorDialog : FlatListIntent

    data class OpenDetail(
        val flatPlatform: FlatPlatform,
        val adId: Long,
        val markAsViewedOnOpen: Boolean = true,
    ) : FlatListIntent

    data object OpenFilter : FlatListIntent
    data object OpenNotifications : FlatListIntent
    data object OpenPremium : FlatListIntent
}

// Action (was FlatListEffect)
sealed interface FlatListAction : MVIAction {
    data object ScrollToTopEffect : FlatListAction
}
