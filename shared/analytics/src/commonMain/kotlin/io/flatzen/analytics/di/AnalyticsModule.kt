package io.flatzen.analytics.di

import io.flatzen.analytics.Analytics
import io.flatzen.analytics.AppMetricaAnalytics
import io.flatzen.analytics.AppMetricaEngine
import io.flatzen.analytics.AppMetricaLauncher
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::AppMetricaEngine)
    singleOf(::AppMetricaLauncher)
    single {
        AppMetricaAnalytics(
            launcher = get(),
            engine = get(),
        )
    } bind Analytics::class
}
