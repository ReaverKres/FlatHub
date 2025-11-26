package io.flatzen.notifications

import kotlinx.coroutines.flow.MutableStateFlow

interface NotificationsService {
    suspend fun getOrCreateDeviceToken(): String?
    suspend fun disable()
    fun platform(): String // "android" | "ios"
}

class InMemoryNotificationsService : NotificationsService {
    private val tokenFlowInternal = MutableStateFlow<String?>(null)

    override suspend fun getOrCreateDeviceToken(): String? = tokenFlowInternal.value

    override suspend fun disable() {
        // no-op default
    }

    override fun platform(): String = "common"
}

