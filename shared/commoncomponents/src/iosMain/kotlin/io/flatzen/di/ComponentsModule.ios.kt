package io.flatzen.di

import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.commoncomponents.network.ConnectionMonitorImpl
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.commoncomponents.utils.DevicePlatformImpl
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalForeignApi::class)
actual fun dataUtilsModule(): Module = module {
    single<ConnectionMonitor> { ConnectionMonitorImpl() }
    single<DevicePlatform> { DevicePlatformImpl() }
}
