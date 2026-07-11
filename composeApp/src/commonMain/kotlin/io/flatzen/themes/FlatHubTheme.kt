package io.flatzen.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.flatzen.commoncomponents.theme.ThemeMode

private val LocalAppShapes = staticCompositionLocalOf { AppShapes() }

@Composable
fun FlatHubTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val shapes = AppShapes()
    val dimens = if (darkTheme) FlatZenDimensDark else FlatZenDimensLight
    val semantic = if (darkTheme) FlatZenSemanticDark else FlatZenSemanticLight
    val colorScheme = if (darkTheme) FlatZenDarkColors else FlatZenLightColors

    CompositionLocalProvider(
        LocalAppShapes provides shapes,
        LocalFlatZenDimens provides dimens,
        LocalFlatZenSemanticColors provides semantic,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FlatZenTypography,
            shapes = shapes.toMaterialShapes(),
            content = content,
        )
    }
}

object FlatHubTheme {
    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current

    val dimens: FlatZenDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalFlatZenDimens.current

    val semantic: FlatZenSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFlatZenSemanticColors.current
}
