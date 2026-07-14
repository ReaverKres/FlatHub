package io.flatzen.viewmodel.swipe

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.viewmodel.list.FlatListScreenState
import io.flatzen.viewmodel.list.UiFlat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

const val SWIPE_AD_DECK_KEY = "swipe-ad-card"

fun UiFlat.swipeDeckKey(): String = "${flatPlatform.name}:$adId"

enum class SwipeOutcome { Liked, Disliked }

@Immutable
data class SwipeUndoEntry(
    val flatPlatform: FlatPlatform,
    val adId: Long,
    val outcome: SwipeOutcome,
) {
    fun deckKey(): String = "${flatPlatform.name}:$adId"
}

@Immutable
sealed interface SwipeDeckItem {
    data class Flat(val value: UiFlat) : SwipeDeckItem
    data object Ad : SwipeDeckItem

    fun deckKey(): String = when (this) {
        is Flat -> value.swipeDeckKey()
        Ad -> SWIPE_AD_DECK_KEY
    }
}

@Immutable
data class SwipeScreenState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val noFlatsToLoadMore: Boolean = false,
    val isAnyFilterApplied: Boolean = false,
    val flatList: ImmutableList<UiFlat> = persistentListOf(),
    val deck: ImmutableList<SwipeDeckItem> = persistentListOf(),
    val pendingDismissKeys: Set<String> = emptySet(),
    val animatingOutKeys: Set<String> = emptySet(),
    val pinnedFrontKey: String? = null,
    val adCardQueued: Boolean = false,
    /** Front flat key the ad stays behind until that card is swiped away. */
    val adAnchorFlatKey: String? = null,
    val undoStack: ImmutableList<SwipeUndoEntry> = persistentListOf(),
    val cityName: String = "",
) : MVIState {
    val isSearchLoading: Boolean get() = isLoading || isRefreshing
    val flatDeckSize: Int get() = deck.count { it is SwipeDeckItem.Flat }
    val showUndo: Boolean get() = undoStack.isNotEmpty()

    companion object {
        val Initial = SwipeScreenState()
    }
}

sealed interface SwipeIntent : MVIIntent {
    data object ScreenVisible : SwipeIntent
    data class SyncListState(val listState: FlatListScreenState) : SwipeIntent
    data class BeginCardDismiss(val deckKey: String) : SwipeIntent
    data class CancelCardDismiss(val deckKey: String) : SwipeIntent
    data class SwipeFlat(val flat: UiFlat, val outcome: SwipeOutcome) : SwipeIntent
    data object DismissAdCard : SwipeIntent
    data object UndoLastSwipe : SwipeIntent
    data class PinFrontCard(val deckKey: String) : SwipeIntent
    data class OpenDetail(val flat: UiFlat) : SwipeIntent
    data object OpenFilter : SwipeIntent
    data object OpenPremium : SwipeIntent
}

sealed interface SwipeAction : MVIAction {
    data object None : SwipeAction
}
