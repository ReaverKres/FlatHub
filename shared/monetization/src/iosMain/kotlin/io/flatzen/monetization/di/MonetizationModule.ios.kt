package io.flatzen.monetization.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.IosAppLovinAdService
import io.flatzen.monetization.ads.NoOpAdService
import io.flatzen.monetization.billing.NoOpBillingBridge
import io.flatzen.monetization.billing.PlatformBillingBridge
import io.flatzen.monetization.billing.StoreKit2BillingBridge
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.datastore.createIosPreferencesDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMonetizationModule(): Module = module {
    single<DataStore<Preferences>> { createIosPreferencesDataStore() }

    single<PlatformBillingBridge> {
        val config = get<MonetizationRemoteConfig>()
        if (config.premiumFallbackEnabled) NoOpBillingBridge() else StoreKit2BillingBridge()
    }

    single<AdService> {
        val config = get<MonetizationRemoteConfig>()
        if (config.applovinSdkKey.isBlank() || config.premiumFallbackEnabled) {
            NoOpAdService()
        } else {
            IosAppLovinAdService().also { it.initialize(config.applovinSdkKey) }
        }
    }
}
