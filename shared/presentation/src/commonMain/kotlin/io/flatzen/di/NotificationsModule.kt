package io.flatzen.di

import io.flatzen.notifications.NotificationsService
import io.flatzen.notifications.NotificationsServiceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

fun notificationsModule(): Module = module {
    single<NotificationsService> { NotificationsServiceImpl() }
}
