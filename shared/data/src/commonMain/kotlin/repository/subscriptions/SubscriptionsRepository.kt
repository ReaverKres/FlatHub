package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument
import api.SubscriptionDocument

interface SubscriptionsRepository {
    suspend fun registerDevice(deviceToken: String, platform: String, userId: String? = null): DeviceDocument
    suspend fun saveAndList(request: CreateSubscriptionRequest): List<SubscriptionDocument>
    suspend fun delete(id: String)
}

