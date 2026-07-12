package io.flatzen.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class NotificationsServiceImpl(
    private val pushPlatform: PushNotificationsPlatform,
) : NotificationsService {

    override suspend fun getOrCreateDeviceToken(): String? {
        return withContext(Dispatchers.Default) {
            val cached = pushPlatform.getLastKnownToken()
            if (!cached.isNullOrBlank()) {
                return@withContext cached
            }
            suspendCancellableCoroutine<String?> { cont ->
                pushPlatform.requestToken { token ->
                    if (cont.isActive) {
                        cont.resume(token)
                    }
                }
            }
        }
    }

    override suspend fun disable() {
        pushPlatform.deleteToken()
    }
}
