package io.flatzen.di

import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.commoncomponents.network.ConnectionMonitorImpl
import io.flatzen.commoncomponents.analytics.AnalyticsManagerInterface
import io.flatzen.commoncomponents.analytics.AnalyticsManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun dataUtilsModule(): Module  = module {
    single<ConnectionMonitor> { ConnectionMonitorImpl(get()) }
}

actual fun analyticsModule(): Module = module {
    single<AnalyticsManagerInterface> { AnalyticsManagerImpl() }
}