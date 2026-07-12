package io.flatzen.di

import android.content.Context
import io.flatzen.notifications.AndroidPushNotificationsPlatform
import io.flatzen.notifications.PushNotificationsPlatform
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformNotificationsModule(): Module = module {
    single<PushNotificationsPlatform> {
        AndroidPushNotificationsPlatform(get<Context>())
    }
}
