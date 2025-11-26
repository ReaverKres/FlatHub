package io.flatzen.notifications

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidNotificationsService : NotificationsService {
    override suspend fun getOrCreateDeviceToken(): String? {
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(task.result)
                    } else {
                        cont.resume(null)
                    }
                }
        }
    }

    override suspend fun disable() {
        // Unsubscribe from topics if used
    }

    override fun platform(): String = "android"
}

