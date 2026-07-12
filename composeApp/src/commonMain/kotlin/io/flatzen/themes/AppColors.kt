package io.flatzen.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val FlatZenLightColors = lightColorScheme(
    primary = Color(0xFFBF4F1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x33BF4F1F),
    onPrimaryContainer = Color(0xFF1A120E),
    secondary = Color(0xFF2B64AD),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F0FA),
    onSecondaryContainer = Color(0xFF1A2332),
    tertiary = Color(0xFFC9A227),
    onTertiary = Color(0xFF1A120E),
    tertiaryContainer = Color(0x33C9A227),
    onTertiaryContainer = Color(0xFF1A120E),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF1A2332),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2332),
    surfaceVariant = Color(0xFFF3EDE6),
    onSurfaceVariant = Color(0xFF5C6678),
    outline = Color(0xFFD8D0C8),
    outlineVariant = Color(0xFFE8E2DB),
    scrim = Color(0x66000000),
)

internal val FlatZenDarkColors = darkColorScheme(
    primary = Color(0xFFE07A52),
    onPrimary = Color(0xFF1A120E),
    primaryContainer = Color(0x33E07A52),
    onPrimaryContainer = Color(0xFFF2EDE8),
    secondary = Color(0xFF6B9AD4),
    onSecondary = Color(0xFF0E1520),
    secondaryContainer = Color(0xFF243044),
    onSecondaryContainer = Color(0xFFF2EDE8),
    tertiary = Color(0xFFE0C45A),
    onTertiary = Color(0xFF1A120E),
    tertiaryContainer = Color(0x33E0C45A),
    onTertiaryContainer = Color(0xFFF2EDE8),
    error = Color(0xFFEF5350),
    onError = Color(0xFF1A120E),
    background = Color(0xFF12151C),
    onBackground = Color(0xFFF2EDE8),
    surface = Color(0xFF1C212B),
    onSurface = Color(0xFFF2EDE8),
    surfaceVariant = Color(0xFF262C38),
    onSurfaceVariant = Color(0xFFA8B0BE),
    outline = Color(0xFF3D4554),
    outlineVariant = Color(0xFF2E3542),
    scrim = Color(0x99000000),
)

@Immutable
data class FlatZenSemanticColors(
    val upsellBanner: Color = Color(0xFFBF4F1F),
    val onUpsellBanner: Color = Color(0xFFFFFFFF),
    val premiumDelayHint: Color = Color(0xFFD6EEF9),
    val onPremiumDelayHint: Color = Color(0xFF1A4A6E),
    val mapPinDefault: Color = Color(0xFFD32F2F),
    val mapPinSelected: Color = Color(0xFF43A047),
    val metroBlue: Color = Color(0xFF1976D2),
    val metroRed: Color = Color(0xFFD32F2F),
    val metroGreen: Color = Color(0xFF388E3C),
    val swipeLike: Color = Color(0xFFE53935),
    val swipeDislike: Color = Color(0xFFFFFFFF),
    val photoOverlayScrim: Color = Color(0x66000000),
    val photoOverlayScrimStrong: Color = Color(0x8C000000),
)

internal val FlatZenSemanticLight = FlatZenSemanticColors(
    premiumDelayHint = Color(0xFFD6EEF9),
    onPremiumDelayHint = Color(0xFF1A4A6E),
    photoOverlayScrim = Color(0x66000000),
    photoOverlayScrimStrong = Color(0x8C000000),
)

internal val FlatZenSemanticDark = FlatZenSemanticColors(
    premiumDelayHint = Color(0xFF1E3A52),
    onPremiumDelayHint = Color(0xFFB8DFF0),
    photoOverlayScrim = Color(0x8C000000),
    photoOverlayScrimStrong = Color(0x99000000),
)

internal val LocalFlatZenSemanticColors = staticCompositionLocalOf { FlatZenSemanticLight }

object FlatZenSemantic {
    val colors: FlatZenSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFlatZenSemanticColors.current
}
