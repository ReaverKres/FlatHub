package io.flatzen.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class NativeAdSlotStyle {
    ContentStream,
    AppWall,
}

const val MAX_NATIVE_ADS_PER_BATCH = 5

@Composable
expect fun MrecAdSlot(
    placement: String,
    modifier: Modifier = Modifier,
)

@Composable
expect fun NativeAdSlot(
    placement: String,
    modifier: Modifier = Modifier,
    style: NativeAdSlotStyle = NativeAdSlotStyle.ContentStream,
    batchId: String? = null,
    slotIndex: Int = 0,
    batchSize: Int = 1,
    onAdLoadResult: ((loaded: Boolean) -> Unit)? = null,
)

expect fun clearNativeAdBatch(batchId: String)

@Composable
fun AdSlotPlaceholder(
    modifier: Modifier = Modifier,
    label: String = "Ad",
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
