package io.flatzen.viewmodel.sharedstates

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.viewmodel.filter.MapAreasUi

sealed class DialogType() {
    object ForceUpdate: DialogType()
    object NetworkError: DialogType()
}

@Immutable
data class SearchErrorDialogState(
    val isVisible: Boolean,
    val dialogType: DialogType,
    val title: String,
    val errorInfo: List<ErrorInfo>,
) {
    class ErrorInfo(
        val platform: FlatPlatform,
        val errorMessages: List<String>
    )
}

@Immutable
data class InfoDialogState(
    val isVisible: Boolean,
    val dialogType: DialogType,
    val title: String,
    val description: String,
)

@Immutable
data class SavedAreasDialogState(
    val isVisible: Boolean = false,
    val savedFilters: List<MapAreasUi> = emptyList()
)