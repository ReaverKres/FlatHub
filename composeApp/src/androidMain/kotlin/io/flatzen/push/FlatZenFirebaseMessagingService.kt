package io.flatzen.push

import android.app.NotificationManager
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.flatzen.notifications.PushTokenRefreshNotifier

private const val PUSH_PREFS = "flatzen_push_notifications"
private const val KEY_FCM_TOKEN = "fcm_token"
private const val CHANNEL_ID = "flatzen_general_notifications"

class FlatZenFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PUSH_PREFS, MODE_PRIVATE)
            .edit { putString(KEY_FCM_TOKEN, token) }
        PushTokenRefreshNotifier.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showNotification(message)
    }

    private fun showNotification(message: RemoteMessage) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.ensureNotificationChannel(
            channelId = CHANNEL_ID,
            channelName = "Notifications",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )

        val title = message.notification?.title ?: message.data["title"] ?: getString(
            applicationInfo.labelRes
        )
        val body = message.notification?.body ?: message.data["body"] ?: ""

        val pendingIntent = createMainActivityContentIntent(requestCode = 0)

        val notification = buildNotificationForChannel(
            channelId = CHANNEL_ID,
            title = title,
            body = body,
            contentIntent = pendingIntent,
        )

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
