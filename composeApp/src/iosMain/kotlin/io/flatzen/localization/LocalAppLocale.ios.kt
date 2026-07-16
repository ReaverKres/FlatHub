package io.flatzen.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default =
        (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "ru"
    private val local = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = local.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(LANG_KEY)
        } else {
            defaults.setObject(listOf(new), LANG_KEY)
        }
        return local.provides(new)
    }
}
