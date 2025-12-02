package io.flatzen

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.di.initKoin
import io.flatzen.notifications.NotificationsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import repository.subscriptions.SubscriptionsRepository
import repository.userpreferences.UserPreferencesRepository

object CommonApplication {
    var di: KoinApplication? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration)
        }

        val subscriptionsRepository: SubscriptionsRepository? = di?.koin?.get()
        val notificationsService: NotificationsService? = di?.koin?.get()
        val devicePlatform: DevicePlatform? = di?.koin?.get()
        val userPreferences: UserPreferencesRepository? = di?.koin?.get()

        // Startup registration if allowed (using cache, then refresh)
        appScope.launch(Dispatchers.IO) {
            try {
                val userId = devicePlatform?.deviceId ?: "-1"

                val token = notificationsService?.getOrCreateDeviceToken()
                val platform = devicePlatform?.platformType?.name.orEmpty()
                val registeredUser = subscriptionsRepository?.registerDevice(
                    deviceToken = token,
                    platform = platform,
                    userId = userId
                )
                userPreferences?.setRegistrationStatus(isRegistered = true)
                userPreferences?.setNotificationAvailable(
                    registeredUser?.isNotificationAvailable == true
                )
            } catch (e: Exception) {
                println("startup registerDevice Exception ${e.message}")
            }
        }

        NotifierManager.addListener(object : NotifierManager.Listener {
            override fun onNewToken(token: String) {
                super.onNewToken(token)
                println("onNewToken $token")
                appScope.launch(Dispatchers.IO) {
                    try {
                        subscriptionsRepository?.registerDevice(
                            deviceToken = token,
                            platform = devicePlatform?.platformType?.name.orEmpty(),
                            userId = devicePlatform?.deviceId ?: "-1"
                        )
                    } catch (e: Exception) {
                        println("onNewToken Exception ${e.message}")
                    }
                }
            }

            override fun onPushNotificationWithPayloadData(
                title: String?,
                body: String?,
                data: PayloadData
            ) {
                super.onPushNotificationWithPayloadData(title, body, data)
                println("onPushNotificationWithPayloadData\n title $title\ndata $data")
            }

            override fun onPushNotification(title: String?, body: String?) {
                super.onPushNotification(title, body)
                println("onPushNotification\n title $title\n")
            }
        })
    }
}