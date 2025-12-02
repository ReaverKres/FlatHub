package repository.subscriptions

import api.CreateSubscriptionRequest
import api.DeviceDocument
import api.RegisterDeviceRequest
import api.SubscriptionDocument
import api.SubscriptionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SubscriptionsRepositoryImpl(
    private val api: SubscriptionsApi
) : SubscriptionsRepository {

    override suspend fun registerDevice(
        deviceToken: String?,
        platform: String,
        userId: String
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

    override fun listByDevice(deviceId: String): Flow<List<SubscriptionDocument>> {
        return flow {
            val result  = api.listByDevice(deviceId)
            emit(result)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun deleteById(id: String) {
        api.deleteById(id)
    }
}

