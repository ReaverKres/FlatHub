package io.flatzen.platformtools.di

import io.flatzen.platformtools.notifications.LocalNotificationManager
import io.flatzen.platformtools.notifications.NotificationPermissionProvider
import io.flatzen.platformtools.background.BackgroundWorkManager
import org.koin.dsl.module

actual val platformToolsModule = module {
    single<LocalNotificationManager> { LocalNotificationManager() }
    single<NotificationPermissionProvider> { NotificationPermissionProvider() }
    single<BackgroundWorkManager> { BackgroundWorkManager() }
}