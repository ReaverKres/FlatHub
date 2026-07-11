package io.flatzen.uiExtensions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Brand press feedback: scale down without Material ripple overflow.
 * Use on list/grid cards and primary CTAs for a consistent CMP feel on Android + iOS.
 */
fun Modifier.flatZenClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.98f,
    onClick: () -> Unit,
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
        ) {
            scope.launch {
                scale.animateTo(
                    scaleDown,
                    tween(durationMillis = 80, easing = FastOutSlowInEasing),
                )
                scale.animateTo(
                    1f,
                    tween(durationMillis = 120, easing = FastOutSlowInEasing),
                )
            }
            onClick()
        }
}

fun Modifier.flatZenCtaClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = flatZenClickable(
    enabled = enabled,
    scaleDown = 0.97f,
    onClick = onClick,
)
