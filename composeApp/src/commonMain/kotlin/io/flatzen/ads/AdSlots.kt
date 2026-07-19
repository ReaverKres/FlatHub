package io.flatzen.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class NativeAdSlotStyle {
    ContentStream,
    AppWall,
}

const val MAX_NATIVE_ADS_PER_BATCH = 5

/** Reserved native-ad slot height so wrapContent hosts do not collapse to 0 before fill. */
val NativeAdMinHeight = 160.dp

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
    /**
     * Stable id for a LazyColumn/RecyclerView slot. Keeps the same creative across
     * off-screen dispose/recompose (Appodeal: unregister on scroll out, re-register on return —
     * do not call getNativeAds again for the same slot).
     */
    reuseKey: String? = null,
    batchId: String? = null,
    slotIndex: Int = 0,
    batchSize: Int = 1,
    onAdLoadResult: ((loaded: Boolean) -> Unit)? = null,
)

expect fun clearNativeAdBatch(batchId: String)

/** Drop cached native creatives retained by [NativeAdSlot] reuseKey (e.g. leave Home feed). */
expect fun clearNativeAdReuseCache()

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
