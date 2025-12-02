package io.flatzen.usecases

import api.DeviceDocumentResponse
import io.flatzen.commoncomponents.utils.DevicePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import repository.subscriptions.SubscriptionsRepository
import repository.userpreferences.UserPreferencesRepository

class RegistrationUseCase(
    private val devicePlatform: DevicePlatform,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val userPreferences: UserPreferencesRepository
) {
    suspend fun registerUser(deviceToken: String?): DeviceDocumentResponse {
        return withContext(Dispatchers.IO) {
            val userId = devicePlatform.deviceId
            val token = deviceToken
            val platform = devicePlatform.platformType.name
            val registeredUser = subscriptionsRepository.registerDevice(
                deviceToken = token,
                platform = platform,
                userId = userId
            )
            registeredUser.let { userPreferences.setUser(it) }
            registeredUser
        }
    }
}