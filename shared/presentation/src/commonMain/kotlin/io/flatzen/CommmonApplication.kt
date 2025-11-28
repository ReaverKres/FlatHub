package io.flatzen

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.di.initKoin
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import repository.subscriptions.SubscriptionsRepository

object CommonApplication {
    var di: KoinApplication? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration)
        }

        val subscriptionsRepository: SubscriptionsRepository? = di?.koin?.get()
        val devicePlatform: DevicePlatform? = di?.koin?.get()

        NotifierManager.addListener(object : NotifierManager.Listener {
            override fun onNewToken(token: String) {
                super.onNewToken(token)
                println("onNewToken $token" )
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        subscriptionsRepository?.registerDevice(
                            deviceToken = token,
                            platform = devicePlatform?.platformType?.name.orEmpty(),
                            userId = devicePlatform?.deviceId
                        )
                    } catch (e: Exception) {
                        println("onNewToken Exception ${e.message}" )
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