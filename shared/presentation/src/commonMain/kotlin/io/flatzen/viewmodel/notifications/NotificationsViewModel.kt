package io.flatzen.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import api.CreateSubscriptionRequest
import com.mmk.kmpnotifier.notification.NotifierManager
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.notifications.NotificationsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.subscriptions.SubscriptionsRepository

class NotificationsViewModel(
    private val permissionsController: PermissionsController,
    private val notificationsService: NotificationsService,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val filterRepository: FilterRepository,
    private val devicePlatform: DevicePlatform
) : ViewModel() {

    init {
        // Listen for token changes and forward to backend
        NotifierManager.addListener(object : NotifierManager.Listener {
            override fun onNewToken(token: String) {
                super.onNewToken(token)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        subscriptionsRepository.registerDevice(
                            deviceToken = token,
                            platform = devicePlatform.platformType.name,
                            userId = null
                        )
                    }
                }
            }
        })
    }

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
                val platform = devicePlatform.platformType.name
                if (token != null) {
                    try {
                        // Register device
                        subscriptionsRepository.registerDevice(
                            deviceToken = token,
                            platform = platform,
                            userId = devicePlatform.deviceId
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
                    } catch (e: Exception) {
                        println("onToggleNotifications Exception: ${e.message}")
                    }
                }
            }
        }
    }
}

