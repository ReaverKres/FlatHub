package io.flatzen.viewmodel.notifications

import api.CreateSubscriptionRequest
import api.toSubscriptionDto
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.commoncomponents.AppFeatures
import io.flatzen.notifications.NotificationsService
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.subscriptions.SubscriptionsRepository

private typealias Ctx = PipelineContext<ToggleNotificationsState, ToggleNotificationsIntent, ToggleNotificationsAction>

class ToggleNotificationsContainer(
    private val permissionsController: PermissionsController,
    private val notificationsService: NotificationsService,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val filterRepository: FilterRepository,
    private val devicePlatform: DevicePlatform
) : Container<ToggleNotificationsState, ToggleNotificationsIntent, ToggleNotificationsAction> {

    override val store = store<ToggleNotificationsState, ToggleNotificationsIntent, ToggleNotificationsAction>(
        initial = ToggleNotificationsState
    ) {
        reduce { intent ->
            when (intent) {
                is ToggleNotificationsIntent.ToggleNotifications -> handleToggleNotifications(intent)
            }
        }
    }

    private suspend fun Ctx.handleToggleNotifications(intent: ToggleNotificationsIntent.ToggleNotifications) {
        if (!AppFeatures.Notifications.ENABLED) return
        val (filterName, enabled) = intent
        if (enabled) {
            try {
                permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
            } catch (_: DeniedAlwaysException) {
                action(ToggleNotificationsAction.ShowSettingsDialog)
                return
            } catch (_: DeniedException) {
                return
            }
            val token = notificationsService.getOrCreateDeviceToken()
            val platform = devicePlatform.platformType.name
            if (token != null) {
                try {
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
