package io.flatzen.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Same tap zones as SwipeScreen: 35% left / 30% center / 35% right. */
private const val LEFT_ZONE_WEIGHT = 0.35f
private const val CENTER_ZONE_WEIGHT = 0.30f
private const val RIGHT_ZONE_WEIGHT = 0.35f

private fun dispatchPhotoTap(
    offsetX: Float,
    width: Float,
    onTapLeft: () -> Unit,
    onTapCenter: () -> Unit,
    onTapRight: () -> Unit,
) {
    if (width <= 0f) return
    val fraction = offsetX / width
    when {
        fraction < LEFT_ZONE_WEIGHT -> onTapLeft()
        fraction > LEFT_ZONE_WEIGHT + CENTER_ZONE_WEIGHT -> onTapRight()
        else -> onTapCenter()
    }
}

/**
 * Tap-zone handler on the same node as [HorizontalPager] so swipes still reach the pager.
 * Use this instead of the [PhotoTapZones] overlay when the pager must support swipe gestures.
 */
fun Modifier.photoTapZones(
    onTapLeft: () -> Unit,
    onTapCenter: () -> Unit,
    onTapRight: () -> Unit,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    return pointerInput(onTapLeft, onTapCenter, onTapRight) {
        detectTapGestures { offset ->
            dispatchPhotoTap(
                offsetX = offset.x,
                width = size.width.toFloat(),
                onTapLeft = onTapLeft,
                onTapCenter = onTapCenter,
                onTapRight = onTapRight,
            )
        }
    }
}

/** Overlay tap zones for static photo views (e.g. SwipeScreen) where pager swipe is not needed. */
@Composable
fun PhotoTapZones(
    modifier: Modifier = Modifier,
    onTapLeft: () -> Unit,
    onTapCenter: () -> Unit,
    onTapRight: () -> Unit,
    enabled: Boolean = true,
) {
    if (!enabled) return

    val noClickIndication = null
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(LEFT_ZONE_WEIGHT)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = noClickIndication,
                    onClick = onTapLeft,
                ),
        )
        Box(
            modifier = Modifier
                .weight(CENTER_ZONE_WEIGHT)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = noClickIndication,
                    onClick = onTapCenter,
                ),
        )
        Box(
            modifier = Modifier
                .weight(RIGHT_ZONE_WEIGHT)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = noClickIndication,
                    onClick = onTapRight,
                ),
        )
    }
}
