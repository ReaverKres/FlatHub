package io.flatzen.viewmodel.notifications

import api.CreateSubscriptionRequest
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import io.flatzen.notifications.NotificationsService
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.launch
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.subscriptions.SubscriptionsRepository

class NotificationsViewModel(
    private val permissionsController: PermissionsController,
    private val notificationsService: NotificationsService,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val filterRepository: FilterRepository
) : BaseMviViewModel<Unit, Unit, Unit, Unit>() {

    override fun initialState(): Unit = Unit

    fun onToggleNotifications(enabled: Boolean) {
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
                val platform = notificationsService.platform()
                if (token != null) {
                    // Register device
                    subscriptionsRepository.registerDevice(
                        deviceToken = token,
                        platform = platform,
                        userId = null
                    )
                    // Send current filter
                    val currentFilter = filterRepository.lastFilter().copy(isNotificationEnabled = true)
                    subscriptionsRepository.saveAndList(
                        CreateSubscriptionRequest(
                            deviceId = token,
                            name = null,
                            filter = currentFilter
                        )
                    )
                }
            } else {
                notificationsService.disable()
                // If you track subscription IDs, call delete(id) here
            }
        }
    }
}

