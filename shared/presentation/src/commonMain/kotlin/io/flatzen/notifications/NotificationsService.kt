package io.flatzen.notifications

import kotlinx.coroutines.flow.MutableSharedFlow

interface NotificationsService {
    val notificationClickListener: MutableSharedFlow<Boolean?>
    suspend fun getOrCreateDeviceToken(): String?
    suspend fun disable()
}

