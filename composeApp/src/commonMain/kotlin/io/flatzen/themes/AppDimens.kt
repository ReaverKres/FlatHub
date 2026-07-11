package io.flatzen.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FlatZenDimens(
    val screenHorizontal: Dp = 16.dp,
    val screenHorizontalCompact: Dp = 12.dp,
    val screenVertical: Dp = 8.dp,
    val listItemSpacing: Dp = 12.dp,
    val cardPadding: Dp = 12.dp,
    val cardPaddingCompact: Dp = 8.dp,
    val sectionSpacing: Dp = 24.dp,
    val topBarHeight: Dp = 56.dp,
    val bottomNavHeight: Dp = 80.dp,
    val fabSize: Dp = 56.dp,
    val fabMargin: Dp = 16.dp,
    val fabSafeZone: Dp = 72.dp,
    val ctaHeight: Dp = 52.dp,
    val ctaRadius: Dp = 14.dp,
    val stickyBarHeight: Dp = 72.dp,
    val photoRatioList: Float = 0.55f,
    val photoRatioGrid: Float = 0.60f,
    val swipePhotoMinRatio: Float = 0.72f,
    val elevationCard: Dp = 2.dp,
    val elevationCardPressed: Dp = 6.dp,
    val elevationFab: Dp = 6.dp,
    val elevationStickyCta: Dp = 8.dp,
    val cardBorderWidth: Dp = 1.dp,
)

internal val FlatZenDimensLight = FlatZenDimens()

internal val FlatZenDimensDark = FlatZenDimens(
    elevationCard = 0.dp,
    elevationCardPressed = 2.dp,
    elevationFab = 4.dp,
    elevationStickyCta = 4.dp,
)

internal val LocalFlatZenDimens = staticCompositionLocalOf { FlatZenDimensLight }

object FlatZenDimensTokens {
    val current: FlatZenDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalFlatZenDimens.current
}
