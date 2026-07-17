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
 * the selected language (e.g. LanguageScreen) does not flash SYSTEM while
 * repository Flows re-subscribe with their initialValue.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.SYSTEM }

/**
 * Provides locale and remounts content when [AppLanguage.tag] changes so
 * Compose resources pick up the new language.
 *
 * Remount unsubscribes Home briefly; feed search must run on the store pipeline
 * ([io.flatzen.viewmodel.list.FlatSearchContainer]) so it is not cancelled mid-flight.
 */
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
