package io.flatzen.monetization.di

import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.billing.SubscriptionService
import io.flatzen.monetization.billing.SubscriptionServiceImpl
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.config.resolveMonetizationConfig
import io.flatzen.monetization.crypto.createPlatformCipher
import io.flatzen.monetization.datastore.EncryptedSecureStore
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.monetization.tier.UserTierProviderImpl
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformMonetizationModule(): Module

fun monetizationCommonModule(): Module = module {
    single { createPlatformCipher() }
    single {
        EncryptedSecureStore(
            dataStore = get(),
            cipher = get(),
        )
    }
    factory<MonetizationRemoteConfig> {
        get<ConfigFieldsChecker>().resolveMonetizationConfig()
    }
    single<SubscriptionService> {
        SubscriptionServiceImpl(
            secureStore = get(),
            bridge = get(),
            premiumFallbackEnabled = { get<MonetizationRemoteConfig>().premiumFallbackEnabled },
            trialDays = { get<MonetizationRemoteConfig>().trialDays },
        )
    }
    single<UserTierProvider> {
        UserTierProviderImpl(
            subscriptionService = get(),
            configProvider = { get() },
        )
    }
}

fun monetizationModules(): List<Module> = listOf(
    monetizationCommonModule(),
    platformMonetizationModule(),
)
