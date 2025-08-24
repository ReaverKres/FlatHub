package io.flatzen.platformtools.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of NotificationPermissionProvider using UNUserNotificationCenter
 */
actual class NotificationPermissionProvider {
    
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    
    actual suspend fun requestPermission(): Boolean = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                completionHandler = { granted, error ->
                    continuation.resume(granted)
                }
            )
        }
    }
    
    actual suspend fun hasPermission(): Boolean = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val hasPermission = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                continuation.resume(hasPermission)
            }
        }
    }
    
    actual fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        settingsUrl?.let { url ->
            if (UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url)
            }
        }
    }
}