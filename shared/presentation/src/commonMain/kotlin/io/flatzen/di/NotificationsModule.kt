package io.flatzen.di

import io.flatzen.notifications.NotificationsService
import io.flatzen.notifications.NotificationsServiceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

fun notificationsModule(): Module = module {
    includes(platformNotificationsModule())
    single<NotificationsService> { NotificationsServiceImpl(get()) }
}

expect fun platformNotificationsModule(): Module
