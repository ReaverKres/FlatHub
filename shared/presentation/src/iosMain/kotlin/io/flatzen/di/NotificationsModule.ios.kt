package io.flatzen.di

import io.flatzen.notifications.IosPushNotificationsPlatform
import io.flatzen.notifications.PushNotificationsPlatform
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformNotificationsModule(): Module = module {
    single<PushNotificationsPlatform> { IosPushNotificationsPlatform() }
}
