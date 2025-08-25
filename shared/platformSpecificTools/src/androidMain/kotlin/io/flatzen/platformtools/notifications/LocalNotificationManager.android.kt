package io.flatzen.platformtools.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of LocalNotificationManager
 * Handles notification permissions for Android 13+ and SecurityException handling
 */
actual class LocalNotificationManager(private val context: Context) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val channelId = "flatzen_notifications"
    private val channelName = "FlatZen Notifications"
    
    init {
        createNotificationChannel()
    }
    
    actual suspend fun showNotification(
        id: String,
        title: String,
        body: String,
        data: Map<String, String>?
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Double permission check as per specification
            if (!checkNotificationPermission()) {
                return@withContext Result.failure(
                    SecurityException("Notification permission not granted")
                )
            }
            
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            // Check permission immediately before notify() as per specification
            if (checkNotificationPermission()) {
                notificationManager.notify(id.hashCode(), notification)
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Notification permission check failed before notify"))
            }
        } catch (e: SecurityException) {
            // Handle SecurityException as per specification
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    actual suspend fun cancelNotification(id: String) {
        try {
            if (checkNotificationPermission()) {
                notificationManager.cancel(id.hashCode())
            }
        } catch (e: SecurityException) {
            // Silently handle SecurityException for cancel operations
        }
    }
    
    actual suspend fun cancelAllNotifications() {
        try {
            if (checkNotificationPermission()) {
                notificationManager.cancelAll()
            }
        } catch (e: SecurityException) {
            // Silently handle SecurityException for cancel operations
        }
    }
    
    /**
     * Private method to check notification permissions as per specification
     * Handles both Android 13+ POST_NOTIFICATIONS and older versions
     */
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - check POST_NOTIFICATIONS permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Older versions - use NotificationManagerCompat
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new apartment listings"
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates a notification channel with custom parameters
     */
    fun createNotificationChannel(
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        description: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                // Use IMPORTANCE_HIGH for foreground services as per memory specifications
                if (channelId.contains("background_work")) NotificationManager.IMPORTANCE_HIGH else importance
            ).apply {
                description?.let { this.description = it }
                // Enable sound and vibration for better visibility
                if (channelId.contains("background_work")) {
                    enableVibration(false)
                    setSound(null, null) // Silent for background work
                } else {
                    enableVibration(true)
                }
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Creates a ForegroundInfo for WorkManager with cancel action
     */
    fun createForegroundInfo(
        notificationId: Int,
        channelId: String,
        title: String,
        text: String,
        cancelIntent: PendingIntent?
    ): ForegroundInfo {
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            
        cancelIntent?.let {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                it
            )
        }
        
        return ForegroundInfo(notificationId, builder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}