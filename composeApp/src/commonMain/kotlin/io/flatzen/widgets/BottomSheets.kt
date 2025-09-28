package io.flatzen.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.screens.map.FlatItemContent
import io.flatzen.viewmodel.list.UiFlat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    selectedSortOption: FlatSort,
    onOptionSelected: (FlatSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        SortOptionRadioButtons(selectedSortOption) {
            onOptionSelected(it)
            onDismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenWithFlatModalSheet(
    selectedFlat: UiFlat?,
    onFlatSelected: (UiFlat?) -> Unit,
    clickOnFavorite: (UiFlat) -> Unit,
    navigateToDetails: (FlatPlatform, Long) -> Unit,
    mapContent: @Composable () -> Unit
) {
    val open = selectedFlat != null
    Box(Modifier.fillMaxSize()) {
        mapContent()

        if (open) {
            ModalBottomSheet(
                onDismissRequest = { onFlatSelected(null) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FlatItemContent(
                        flat = selectedFlat,
                        onClick = {
                            onFlatSelected(null)
                            navigateToDetails(selectedFlat.flatPlatform, selectedFlat.adId)
                        },
                        clickOnFavorite = { clickOnFavorite(selectedFlat) }
                    )
                    selectedFlat.coordinates?.let {
                        OpenInMapButton(
                            coordinates = it,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}