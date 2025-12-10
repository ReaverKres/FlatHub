package io.flatzen

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.flatzen.di.initKoin
import io.flatzen.notifications.NotificationsService
import io.flatzen.usecases.RegistrationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

object CommonApplication {
    var di: KoinApplication? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration)
        }

        val notificationsService: NotificationsService? = di?.koin?.get()
        val registrationUseCase: RegistrationUseCase? = di?.koin?.get()

        // Startup registration if allowed (using cache, then refresh)
        appScope.launch(Dispatchers.IO) {
            try {
                val token = notificationsService?.getOrCreateDeviceToken()
                registrationUseCase?.registerUser(token)
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
                        registrationUseCase?.registerUser(token)
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
                println("myapplication onPushNotificationWithPayloadData\n title $title\ndata $data")
            }

            override fun onPushNotification(title: String?, body: String?) {
                super.onPushNotification(title, body)
                println("myapplication onPushNotification\n title $title\n")
            }

            override fun onNotificationClicked(data: PayloadData) {
                super.onNotificationClicked(data)
                println("myapplication onNotificationClicked\n data = $data\n")
                appScope.launch {
                    notificationsService?.notificationClickListener?.emit(true)
                }
            }
        })
    }
}