package io.flatzen.platformtools.di

import android.content.Context
import io.flatzen.platformtools.notifications.LocalNotificationManager
import io.flatzen.platformtools.notifications.NotificationPermissionProvider
import io.flatzen.platformtools.background.BackgroundWorkManager
import org.koin.dsl.module

actual val platformToolsModule = module {
    single<LocalNotificationManager> { LocalNotificationManager(get<Context>()) }
    single<NotificationPermissionProvider> { NotificationPermissionProvider(get<Context>()) }
    single<BackgroundWorkManager> { BackgroundWorkManager(get<Context>()) }
}