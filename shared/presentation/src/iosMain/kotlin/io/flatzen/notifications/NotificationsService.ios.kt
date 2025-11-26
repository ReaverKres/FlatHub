package io.flatzen.notifications

class IOSNotificationsService : NotificationsService {
    override suspend fun getOrCreateDeviceToken(): String? {
        // APNs token will be obtained after registration in AppDelegate; return null for now
        return null
    }

    override suspend fun disable() {
        // No-op; cannot revoke system permission programmatically
    }

    override fun platform(): String = "ios"
}

