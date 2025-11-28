package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument
import api.RegisterDeviceRequest
import api.SubscriptionDocument
import api.SubscriptionsApi

class SubscriptionsRepositoryImpl(
    private val api: SubscriptionsApi
) : SubscriptionsRepository {

    override suspend fun registerDevice(
        deviceToken: String,
        platform: String,
        userId: String?
    ): DeviceDocument {

        return api.register(
                RegisterDeviceRequest(
                    deviceToken = deviceToken, platform = platform, userId = userId
                )
        )
    }

    override suspend fun saveAndList(request: CreateSubscriptionRequest): List<SubscriptionDocument> {
        return api.saveAndList(request)
    }

    override suspend fun delete(id: String) {
        api.delete(id)
    }
}

