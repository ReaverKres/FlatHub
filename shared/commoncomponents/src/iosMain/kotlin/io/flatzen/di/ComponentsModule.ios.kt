package io.flatzen.di

import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AnalyticsManagerImpl
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalForeignApi::class)
actual fun dataUtilsModule(): Module = module {
}

actual fun analyticsModule(): Module = module {
    single<AnalyticsManager> { AnalyticsManagerImpl() }
}