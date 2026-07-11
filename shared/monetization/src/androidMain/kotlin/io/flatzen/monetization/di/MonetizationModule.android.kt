package io.flatzen.monetization.di

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppLovinAdService
import io.flatzen.monetization.ads.NoOpAdService
import io.flatzen.monetization.billing.NoOpBillingBridge
import io.flatzen.monetization.billing.PlatformBillingBridge
import io.flatzen.monetization.billing.PlayBillingBridge
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.datastore.createAndroidPreferencesDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMonetizationModule(): Module = module {
    single<DataStore<Preferences>> {
        createAndroidPreferencesDataStore(get())
    }

    single<() -> Activity?> {
        // Overridden in androidApp with real Activity holder if available
        { null }
    }

    single<PlatformBillingBridge> {
        val config = get<MonetizationRemoteConfig>()
        if (config.premiumFallbackEnabled) {
            NoOpBillingBridge()
        } else {
            PlayBillingBridge(get(), get())
        }
    }

    single<AdService> {
        val config = get<MonetizationRemoteConfig>()
        if (config.applovinSdkKey.isBlank() || config.premiumFallbackEnabled) {
            NoOpAdService()
        } else {
            AppLovinAdService(get(), get()).also { it.initialize(config.applovinSdkKey) }
        }
    }
}
