package io.flatzen

import io.flatzen.analytics.Analytics
import io.flatzen.di.initKoin
import io.flatzen.notifications.NotificationsService
import io.flatzen.notifications.PushTokenRefreshNotifier
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
        val analytics: Analytics? = di?.koin?.get()

        analytics?.activate()

        PushTokenRefreshNotifier.register { token ->
            appScope.launch(Dispatchers.IO) {
                runCatching { registrationUseCase?.registerUser(token) }
            }
        }

        appScope.launch(Dispatchers.IO) {
            try {
                val token = notificationsService?.getOrCreateDeviceToken()
                registrationUseCase?.registerUser(token)
            } catch (e: Exception) {
                println("startup registerDevice Exception ${e.message}")
            }
        }
    }
}
