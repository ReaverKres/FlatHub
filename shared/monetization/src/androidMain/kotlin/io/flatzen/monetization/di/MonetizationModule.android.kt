package io.flatzen.monetization.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppodealAdService
import io.flatzen.monetization.ads.NoOpAdService
import io.flatzen.monetization.billing.NoOpBillingBridge
import io.flatzen.monetization.billing.PlatformBillingBridge
import io.flatzen.monetization.billing.PlayBillingBridge
import io.flatzen.monetization.datastore.createAndroidPreferencesDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMonetizationModule(): Module = module {
    single<DataStore<Preferences>> {
        createAndroidPreferencesDataStore(get())
    }

    single<PlatformBillingBridge>(createdAtStart = true) {
        if (MonetizationDefaults.PREMIUM_FALLBACK_ENABLED) {
            NoOpBillingBridge()
        } else {
            PlayBillingBridge(get())
        }
    }

    single<AdService>(createdAtStart = true) {
        get<PlatformBillingBridge>() // billing must connect before Appodeal init
        if (MonetizationDefaults.PREMIUM_FALLBACK_ENABLED ||
            MonetizationDefaults.APPODEAL_ANDROID_APP_KEY.isBlank()
        ) {
            NoOpAdService()
        } else {
            AppodealAdService(
                context = get(),
                androidAppKey = MonetizationDefaults.APPODEAL_ANDROID_APP_KEY,
            ).also {
                it.initialize(
                    MonetizationDefaults.APPODEAL_ANDROID_APP_KEY,
                    MonetizationDefaults.APPODEAL_IOS_APP_KEY,
                )
            }
        }
    }
}
