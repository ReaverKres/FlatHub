package io.flatzen.firebase

object RemoteConfigNativeHandlers {
    var initHandler: ((Long) -> Unit)? = null
    var fetchAndActivateHandler: (((Boolean) -> Unit) -> Unit)? = null
    var getStringHandler: ((String) -> String)? = null
    var getLongHandler: ((String) -> String)? = null
    var getBoolHandler: ((String) -> String)? = null
    var isLoadedHandler: (() -> Boolean)? = null
}
