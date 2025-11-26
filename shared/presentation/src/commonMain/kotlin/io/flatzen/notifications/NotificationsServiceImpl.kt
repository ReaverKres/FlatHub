package io.flatzen.notifications

import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationsServiceImpl : NotificationsService {
    override suspend fun getOrCreateDeviceToken(): String? {
        return withContext(Dispatchers.Default) {
            runCatching { NotifierManager.getPushNotifier().getToken() }.getOrNull()
        }
    }

    override suspend fun disable() {
        runCatching { NotifierManager.getPushNotifier().deleteMyToken() }
    }
}

