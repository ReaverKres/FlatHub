package io.flatzen.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

internal fun NotificationManager.ensureNotificationChannel(
    channelId: String,
    channelName: String,
    importance: Int,
) {
    if (getNotificationChannel(channelId) != null) return
    createNotificationChannel(NotificationChannel(channelId, channelName, importance))
}

internal fun Context.createMainActivityContentIntent(
    requestCode: Int,
): PendingIntent {
    val intent = Intent().apply {
        setClassName(packageName, "io.flatzen.MainActivity")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        this,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

internal fun Context.buildNotificationForChannel(
    channelId: String,
    title: String,
    body: String,
    contentIntent: PendingIntent,
): Notification {
    return NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(contentIntent)
        .build()
}
