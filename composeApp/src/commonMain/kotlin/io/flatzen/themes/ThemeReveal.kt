package io.flatzen.themes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import io.flatzen.commoncomponents.theme.ThemeMode
import io.flatzen.utils.PlatformSystemBars
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val CIRCLE_SEGMENTS = 64
private const val TWO_PI = 2f * PI.toFloat()

val LocalThemeRevealController = staticCompositionLocalOf<ThemeRevealController> {
    error("ThemeRevealController not provided")
}

data class ThemeRevealRequest(
    val originInRoot: Offset,
    val targetMode: ThemeMode,
)

class ThemeRevealController {
    var request: ThemeRevealRequest? by mutableStateOf(null)
        private set

    val isAnimating: Boolean
        get() = request != null

    val pendingMode: ThemeMode?
        get() = request?.targetMode

    fun start(originInRoot: Offset, targetMode: ThemeMode) {
        if (request != null) return
        request = ThemeRevealRequest(originInRoot = originInRoot, targetMode = targetMode)
    }

    internal fun clear() {
        request = null
    }
}

@Composable
fun rememberThemeRevealController(): ThemeRevealController =
    remember { ThemeRevealController() }

/**
 * App-level theme reveal: dual-composes [content] (Scaffold + bottom bar) while animating.
 * Navigation state / side-effects must stay outside this host.
 */
@Composable
fun ThemeRevealHost(
    controller: ThemeRevealController,
    committedMode: ThemeMode,
    isDark: Boolean,
    onCommit: suspend (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Int = 1000,
    edgeColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    var hostOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }
    var showingReverse by remember { mutableStateOf(false) }

    val timeState = remember { mutableFloatStateOf(0f) }
    val progressState = remember { mutableFloatStateOf(0f) }
    val clipPathObj = remember { Path() }
    val edgePath = remember { Path() }

    val request = controller.request

    // Status / nav bar icon contrast follows committed theme (snaps on commit).
    PlatformSystemBars(isDark = isDark)

    LaunchedEffect(request) {
        val req = request ?: return@LaunchedEffect
        val targetDark = req.targetMode.resolveDark(systemDark)
        if (targetDark == isDark) {
            onCommit(req.targetMode)
            controller.clear()
            return@LaunchedEffect
        }

        tapOffset = Offset(
            x = req.originInRoot.x - hostOriginInRoot.x,
            y = req.originInRoot.y - hostOriginInRoot.y,
        )
        showingReverse = true
        val start = withFrameNanos { it }
        while (true) {
            val elapsedMs = (withFrameNanos { it } - start) / 1_000_000f
            val raw = (elapsedMs / durationMs).coerceAtMost(1f)
            progressState.floatValue = FastOutSlowInEasing.transform(raw)
            timeState.floatValue = elapsedMs / 2000f * TWO_PI
            if (raw >= 1f) break
        }
        onCommit(req.targetMode)
        showingReverse = false
        progressState.floatValue = 0f
        timeState.floatValue = 0f
        controller.clear()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                hostOriginInRoot = coords.positionInRoot()
                size = coords.size
            },
    ) {
        if (!showingReverse) {
            FlatHubTheme(themeMode = committedMode) {
                content()
            }
        } else {
            FlatHubTheme(themeMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT) {
                content()
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val progress = progressState.floatValue
                        val time = timeState.floatValue
                        val currentRadius = maxReveal(tapOffset, size) * progress
                        fillWobblyPath(clipPathObj, tapOffset, currentRadius, time, 1f)
                        clipPath(clipPathObj) {
                            this@drawWithContent.drawContent()
                        }
                    },
            ) {
                FlatHubTheme(themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK) {
                    content()
                }
            }

            if (edgeColor != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val progress = progressState.floatValue
                    val time = timeState.floatValue
                    val currentRadius = maxReveal(tapOffset, size) * progress
                    if (progress <= 0f || progress >= 1f) return@Canvas
                    fillWobblyPath(edgePath, tapOffset, currentRadius, time, 1f)
                    drawPath(
                        path = edgePath,
                        color = edgeColor.copy(alpha = 0.35f * (1f - progress)),
                        style = Stroke(width = 2.5f),
                    )
                }
            }
        }
    }
}

@Composable
fun ProvideThemeRevealController(
    controller: ThemeRevealController = rememberThemeRevealController(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalThemeRevealController provides controller) {
        content()
    }
}

fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun maxReveal(center: Offset, size: IntSize): Float = hypot(
    maxOf(center.x, size.width - center.x).toFloat(),
    maxOf(center.y, size.height - center.y).toFloat(),
)

private fun fillWobblyPath(
    path: Path,
    center: Offset,
    radius: Float,
    time: Float,
    intensity: Float,
) {
    path.reset()
    if (radius <= 0f) return
    for (i in 0..CIRCLE_SEGMENTS) {
        val angle = (i.toFloat() / CIRCLE_SEGMENTS) * TWO_PI
        val wobble = 1f +
                (sin(angle * 7f + time * 1.2f) * 0.03f +
                        cos(angle * 5f - time * 1.9f) * 0.025f +
                        sin(angle * 11f + time * 3f) * 0.015f) * intensity
        val r = radius * wobble
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}
