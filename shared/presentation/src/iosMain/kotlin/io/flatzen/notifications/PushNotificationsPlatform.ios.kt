package io.flatzen.notifications

import platform.Foundation.NSTimer
import platform.Foundation.NSUserDefaults

private const val FCM_TOKEN_KEY = "fcm_registration_token"

class IosPushNotificationsPlatform : PushNotificationsPlatform {

    override fun requestToken(onResult: (String?) -> Unit) {
        val existing = getLastKnownToken()
        if (!existing.isNullOrBlank()) {
            onResult(existing)
            return
        }
        pollToken(attempt = 0, maxAttempts = 6, onResult = onResult)
    }

    override fun getLastKnownToken(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(FCM_TOKEN_KEY)
    }

    override suspend fun deleteToken() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(FCM_TOKEN_KEY)
    }

    private fun pollToken(
        attempt: Int,
        maxAttempts: Int,
        onResult: (String?) -> Unit,
    ) {
        val tokenNow = getLastKnownToken()
        if (!tokenNow.isNullOrBlank() || attempt >= maxAttempts) {
            onResult(tokenNow)
            return
        }
        NSTimer.scheduledTimerWithTimeInterval(
            interval = 0.5,
            repeats = false,
        ) {
            pollToken(
                attempt = attempt + 1,
                maxAttempts = maxAttempts,
                onResult = onResult,
            )
        }
    }
}
