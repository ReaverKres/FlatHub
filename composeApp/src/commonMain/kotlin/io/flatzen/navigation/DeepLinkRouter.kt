package io.flatzen.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles deep link URIs and emits parsed Route for navigation.
 * MainActivity emits URIs, App collects and navigates.
 */
object DeepLinkRouter {
    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deepLinks: SharedFlow<String> = _deepLinks

    fun emitDeepLink(uri: String) {
        _deepLinks.tryEmit(uri)
    }
}
