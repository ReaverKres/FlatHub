package io.flatzen.di

import io.flatzen.notifications.AndroidNotificationsService
import io.flatzen.notifications.NotificationsService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun notificationsModule(): Module = module {
    single<NotificationsService> { AndroidNotificationsService() }
}

