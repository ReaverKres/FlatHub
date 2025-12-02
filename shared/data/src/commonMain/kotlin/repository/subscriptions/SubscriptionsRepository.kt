package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocumentResponse
import api.SubscriptionDocument
import kotlinx.coroutines.flow.Flow

interface SubscriptionsRepository {
    suspend fun registerDevice(deviceToken: String? = null, platform: String, userId: String): DeviceDocumentResponse
    suspend fun saveSub(request: CreateSubscriptionRequest)
    fun listByDevice(deviceId: String): Flow<List<SubscriptionDocument>>
    suspend fun deleteById(id: String)
}

