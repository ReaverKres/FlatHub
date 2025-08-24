package io.flatzen.platformtools.notifications

/**
 * Cross-platform local notification manager interface
 */
expect class LocalNotificationManager {
    suspend fun showNotification(
        id: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Result<Unit>
    
    suspend fun cancelNotification(id: String)
    suspend fun cancelAllNotifications()
}