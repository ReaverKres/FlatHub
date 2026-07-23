package io.flatzen.viewmodel.sharedstates

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.viewmodel.filter.MapAreasUi

sealed class DialogType() {
    object ForceUpdate: DialogType()
    object NetworkError: DialogType()
}

@Immutable
data class SearchErrorDialogState(
    val isVisible: Boolean,
    val dialogType: DialogType,
    val title: LocalizationKeys,
    val errorInfo: List<ErrorInfo>,
    val generalError: LocalizationKeys? = null,
    /** Platforms that participated in this search (country-scoped). Empty → fall back to errorInfo only. */
    val searchedPlatforms: List<FlatPlatform> = emptyList(),
    /** Stable id for [io.flatzen.viewmodel.list.SearchNetworkErrorDismissStore]. */
    val fingerprint: String = "",
) {
    class ErrorInfo(
        val platform: FlatPlatform,
        val errorMessages: List<String>
    )

    val platformsForStatusRow: List<FlatPlatform>
        get() = searchedPlatforms.ifEmpty { errorInfo.map { it.platform }.distinct() }
}

@Immutable
data class InfoDialogState(
    val isVisible: Boolean,
    val dialogType: DialogType,
    val title: LocalizationKeys,
    val description: LocalizationKeys,
)

@Immutable
data class SavedAreasDialogState(
    val isVisible: Boolean = false,
    val title: LocalizationKeys? = null,
    val savedAreas: List<MapAreasUi> = emptyList()
)