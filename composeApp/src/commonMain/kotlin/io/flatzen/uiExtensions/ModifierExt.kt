package io.flatzen.uiExtensions

import androidx.compose.ui.Modifier

inline fun Modifier.thenIf(
    condition: Boolean,
    factory: Modifier.() -> Modifier
): Modifier {
    return if (condition) factory.invoke(this) else this
}