package io.flatzen.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * Twinby/Tinder-style swipe card. Right = like, left = dislike.
 * [onSwipeProgress] reports -1..1 for mid-swipe overlays.
 * When [enabled] is false the card stays in the stack without drag gestures
 * (so promoting a back card to front does not remount its content).
 */
@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipeProgress: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val threshold = screenWidthPx * 0.28f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(screenWidthPx) {
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        when {
                                            offsetX.value > threshold -> {
                                                offsetX.animateTo(screenWidthPx * 1.2f)
                                                onSwipedRight()
                                            }

                                            offsetX.value < -threshold -> {
                                                offsetX.animateTo(-screenWidthPx * 1.2f)
                                                onSwipedLeft()
                                            }

                                            else -> {
                                                offsetX.animateTo(
                                                    0f,
                                                    spring(stiffness = Spring.StiffnessMedium),
                                                )
                                                offsetY.animateTo(
                                                    0f,
                                                    spring(stiffness = Spring.StiffnessMedium),
                                                )
                                                onSwipeProgress(0f)
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(0f)
                                        offsetY.animateTo(0f)
                                        onSwipeProgress(0f)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo(offsetY.value + dragAmount.y * 0.35f)
                                        onSwipeProgress(
                                            (offsetX.value / threshold).coerceIn(-1.5f, 1.5f),
                                        )
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .graphicsLayer {
                    translationX = if (enabled) offsetX.value else 0f
                    translationY = if (enabled) offsetY.value else 0f
                    rotationZ = if (enabled) offsetX.value / 60f else 0f
                },
        ) {
            content()
        }
    }
}
