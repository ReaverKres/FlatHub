package io.flatzen.notifications

interface PushNotificationsPlatform {
    fun requestToken(onResult: (String?) -> Unit)
    fun getLastKnownToken(): String?
    suspend fun deleteToken()
}
