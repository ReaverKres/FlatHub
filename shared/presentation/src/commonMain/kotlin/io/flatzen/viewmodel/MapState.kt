package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.viewmodel.filter.SaveDialogState
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

@Immutable
data class MapUiState(
    val isMapAreaActive: Boolean = false,
    val undoBtnVisible: Boolean = true,
    val saveAreaDialogState: SaveDialogState = SaveDialogState(),
) : MVIState {
    companion object {
        val Initial = MapUiState()
    }
}

sealed interface MapIntent : MVIIntent {
    data object Initialize : MapIntent
    data object ClickOnMapArea : MapIntent
    data object CenterOnWorld : MapIntent
    data class AddPointToPath(val x: Double, val y: Double) : MapIntent
    data object UndoLastPoint : MapIntent
    data object ShowSaveAreaDialog : MapIntent
    data object HideSaveAreaDialog : MapIntent
    data class UpdateAreaName(val name: String) : MapIntent
    data object SaveArea : MapIntent

    data class OpenDetail(
        val flatPlatform: io.flatzen.commoncomponents.commonentities.FlatPlatform,
        val adId: Long
    ) : MapIntent

    data object OpenFilter : MapIntent
    data object NavigateBack : MapIntent
    data object OpenPremium : MapIntent
    data object RequestMapAreaOrPremium : MapIntent
}

sealed interface MapAction : MVIAction {
    data class FirstPointInPath(val pathId: String, val x: Double, val y: Double) : MapAction
    data class MapAreaSaved(val pathId: String) : MapAction
}
