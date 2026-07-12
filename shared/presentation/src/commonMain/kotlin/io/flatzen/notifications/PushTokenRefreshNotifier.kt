package io.flatzen.notifications

object PushTokenRefreshNotifier {
    private var callback: ((String) -> Unit)? = null

    fun register(callback: (String) -> Unit) {
        this.callback = callback
    }

    fun onNewToken(token: String) {
        callback?.invoke(token)
    }
}
