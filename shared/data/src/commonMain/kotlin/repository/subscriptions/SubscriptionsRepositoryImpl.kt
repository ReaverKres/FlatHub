package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument
import api.RegisterDeviceRequest
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

    override suspend fun saveSub(request: CreateSubscriptionRequest) {
        api.saveSub(request)
    }
}

