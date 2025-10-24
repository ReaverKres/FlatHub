package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FromToRange

@Immutable
data class CommercialFilters(
    val roomRange: FromToRange?
)