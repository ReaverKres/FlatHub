package io.flatzen.platformtools.notifications

/**
 * Cross-platform notification permission provider interface
 */
expect class NotificationPermissionProvider {
    suspend fun requestPermission(): Boolean
    suspend fun hasPermission(): Boolean
    fun openAppSettings()
}