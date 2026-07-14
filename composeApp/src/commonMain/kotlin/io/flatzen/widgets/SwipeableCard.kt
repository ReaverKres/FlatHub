package io.flatzen.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val EXIT_ANIMATION_MS = 220
private const val EXIT_DISTANCE_FACTOR = 1.35f

/**
 * Twinby/Tinder-style swipe card. Right = like, left = dislike.
 * [onSwipeProgress] reports -1..1 for mid-swipe overlays.
 * When [enabled] is false the card stays in the stack without drag gestures
 * (so promoting a back card to front does not remount its content).
 *
 * [onSwipeWillDismiss] fires once when the drag first crosses the dismiss
 * threshold (synchronously on the gesture thread), so the parent can insert the
 * next deck card (e.g. an ad) behind this one while the gesture continues.
 * [onSwipeDismissCancelled] fires if the user returns below the threshold or
 * cancels the gesture after will-dismiss was notified.
 *
 * Dismiss result callbacks ([onSwipedLeft]/[onSwipedRight]) fire only after the
 * fly-out animation finishes.
 */
@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipeWillDismiss: () -> Unit = {},
    onSwipeDismissCancelled: () -> Unit = {},
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
        val exitDistance = screenWidthPx * EXIT_DISTANCE_FACTOR
        val exitSpec = tween<Float>(
            durationMillis = EXIT_ANIMATION_MS,
            easing = FastOutLinearInEasing,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(screenWidthPx, exitDistance) {
                            var willDismissNotified = false

                            fun notifyWillDismissIfNeeded(absOffset: Float) {
                                val crossed = absOffset >= threshold
                                when {
                                    crossed && !willDismissNotified -> {
                                        willDismissNotified = true
                                        onSwipeWillDismiss()
                                    }

                                    !crossed && willDismissNotified -> {
                                        willDismissNotified = false
                                        onSwipeDismissCancelled()
                                    }
                                }
                            }

                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        when {
                                            offsetX.value > threshold -> {
                                                onSwipeProgress(1f)
                                                if (!willDismissNotified) {
                                                    willDismissNotified = true
                                                    onSwipeWillDismiss()
                                                }
                                                offsetX.animateTo(exitDistance, exitSpec)
                                                onSwipedRight()
                                            }

                                            offsetX.value < -threshold -> {
                                                onSwipeProgress(-1f)
                                                if (!willDismissNotified) {
                                                    willDismissNotified = true
                                                    onSwipeWillDismiss()
                                                }
                                                offsetX.animateTo(-exitDistance, exitSpec)
                                                onSwipedLeft()
                                            }

                                            else -> {
                                                if (willDismissNotified) {
                                                    willDismissNotified = false
                                                    onSwipeDismissCancelled()
                                                }
                                                coroutineScope {
                                                    launch {
                                                        offsetX.animateTo(
                                                            0f,
                                                            spring(stiffness = Spring.StiffnessMedium),
                                                        )
                                                    }
                                                    launch {
                                                        offsetY.animateTo(
                                                            0f,
                                                            spring(stiffness = Spring.StiffnessMedium),
                                                        )
                                                    }
                                                }
                                                onSwipeProgress(0f)
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        if (willDismissNotified) {
                                            willDismissNotified = false
                                            onSwipeDismissCancelled()
                                        }
                                        coroutineScope {
                                            launch { offsetX.animateTo(0f) }
                                            launch { offsetY.animateTo(0f) }
                                        }
                                        onSwipeProgress(0f)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val nextX = offsetX.value + dragAmount.x
                                    val nextY = offsetY.value + dragAmount.y * 0.35f
                                    // Notify before coroutine so BeginCardDismiss is not queued behind
                                    // a backlog of snapTo jobs from rapid drag events.
                                    notifyWillDismissIfNeeded(abs(nextX))
                                    onSwipeProgress((nextX / threshold).coerceIn(-1.5f, 1.5f))
                                    scope.launch {
                                        offsetX.snapTo(nextX)
                                        offsetY.snapTo(nextY)
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
