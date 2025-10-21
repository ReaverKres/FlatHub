package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun LaunchedEffectOnce(
    vararg keys: Any?,
    action: suspend () -> Unit
) {
    val hasExecuted = rememberSaveable(*keys) { mutableStateOf(false) }

    LaunchedEffect(*keys) {
        if (!hasExecuted.value) {
            action()
            hasExecuted.value = true
        }
    }
}