package io.flatzen.platformtools.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.UserNotifications.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of LocalNotificationManager using UNUserNotificationCenter
 */
actual class LocalNotificationManager {
    
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    
    actual suspend fun showNotification(
        id: String,
        title: String,
        body: String,
        data: Map<String, String>?
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Check if notifications are authorized
            val authorized = checkNotificationPermission()
            if (!authorized) {
                return@withContext Result.failure(
                    Exception("Notification permission not granted")
                )
            }
            
            // Create notification content
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
                data?.let { userData ->
                    setUserInfo(userData.mapKeys { it.key } as Map<Any?, *>)
                }
            }
            
            // Create request
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = id,
                content = content,
                trigger = null // Immediate delivery
            )
            
            // Add notification request
            suspendCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    actual suspend fun cancelNotification(id: String) {
        withContext(Dispatchers.Main) {
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
            notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
        }
    }
    
    actual suspend fun cancelAllNotifications() {
        withContext(Dispatchers.Main) {
            notificationCenter.removeAllPendingNotificationRequests()
            notificationCenter.removeAllDeliveredNotifications()
        }
    }
    
    private suspend fun checkNotificationPermission(): Boolean = suspendCoroutine { continuation ->
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            val authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            continuation.resume(authorized)
        }
    }
}