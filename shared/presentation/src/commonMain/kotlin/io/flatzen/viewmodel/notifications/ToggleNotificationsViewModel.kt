package io.flatzen.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import api.CreateSubscriptionRequest
import api.toSubscriptionDto
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.notifications.NotificationsService
import kotlinx.coroutines.launch
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.subscriptions.SubscriptionsRepository

class ToggleNotificationsViewModel(
    private val permissionsController: PermissionsController,
    private val notificationsService: NotificationsService,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val filterRepository: FilterRepository,
    private val devicePlatform: DevicePlatform
) : ViewModel() {

    fun onToggleNotifications(filterName: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                try {
                    permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                } catch (_: DeniedAlwaysException) {
                    return@launch
                } catch (_: DeniedException) {
                    return@launch
                }
                val token = notificationsService.getOrCreateDeviceToken()
                val platform = devicePlatform.platformType.name
                if (token != null) {
                    try {
                        // Send current filter
                        val currentFilter = filterRepository.lastFilter().copy(
                            isNotificationEnabled = true,
                            name = filterName
                        )
                        subscriptionsRepository.saveSub(
                            CreateSubscriptionRequest(
                                deviceId = devicePlatform.deviceId,
                                filter = currentFilter.toSubscriptionDto()
                            )
                        )
                    } catch (e: Exception) {
                        println("onToggleNotifications Exception: ${e.message}")
                    }
                }
            }
        }
    }
}

