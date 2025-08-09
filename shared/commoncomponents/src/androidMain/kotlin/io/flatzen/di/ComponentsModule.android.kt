package io.flatzen.di

import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.commoncomponents.network.ConnectionMonitorImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun dataUtilsModule(): Module  = module {
    single<ConnectionMonitor> { ConnectionMonitorImpl(get()) }
}