package io.flatzen.notifications

interface NotificationsService {
    suspend fun getOrCreateDeviceToken(): String?
    suspend fun disable()
}

