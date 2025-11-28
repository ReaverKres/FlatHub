package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument

interface SubscriptionsRepository {
    suspend fun registerDevice(deviceToken: String, platform: String, userId: String? = null): DeviceDocument
    suspend fun saveSub(request: CreateSubscriptionRequest)
}

