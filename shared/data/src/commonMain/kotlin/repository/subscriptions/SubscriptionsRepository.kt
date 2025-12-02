package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument
import api.SubscriptionDocument
import kotlinx.coroutines.flow.Flow

interface SubscriptionsRepository {
    suspend fun registerDevice(deviceToken: String? = null, platform: String, userId: String): DeviceDocument
    suspend fun saveSub(request: CreateSubscriptionRequest)
    fun listByDevice(deviceId: String): Flow<List<SubscriptionDocument>>
    suspend fun deleteById(id: String)
}

