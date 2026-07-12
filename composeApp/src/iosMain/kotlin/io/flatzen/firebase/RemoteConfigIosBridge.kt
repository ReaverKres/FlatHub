package io.flatzen.firebase

/**
 * Wired from Swift before remote config is used.
 */
object RemoteConfigIosBridge {
    fun configure(
        init: (timeoutSeconds: Long) -> Unit,
        fetchAndActivate: (onComplete: (Boolean) -> Unit) -> Unit,
        getString: (String) -> String,
        getLong: (String) -> String,
        getBool: (String) -> String,
        isLoaded: () -> Boolean,
    ) {
        RemoteConfigNativeHandlers.initHandler = init
        RemoteConfigNativeHandlers.fetchAndActivateHandler = fetchAndActivate
        RemoteConfigNativeHandlers.getStringHandler = getString
        RemoteConfigNativeHandlers.getLongHandler = getLong
        RemoteConfigNativeHandlers.getBoolHandler = getBool
        RemoteConfigNativeHandlers.isLoadedHandler = isLoaded
    }
}
