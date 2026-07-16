package io.flatzen.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import io.flatzen.commoncomponents.theme.AppLanguage

expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/**
 * Survives [key]-based remounts inside [AppLocaleProvider], so UI that reads
 * the selected language (e.g. MoreScreen selector) does not flash SYSTEM while
 * repository Flows re-subscribe with their initialValue.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.SYSTEM }

@Composable
fun AppLocaleProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppLocale provides language.tag,
        LocalAppLanguage provides language,
    ) {
        key(language.tag) {
            content()
        }
    }
}
