package io.flatzen.translation.di

import io.flatzen.translation.AzureTranslationProvider
import io.flatzen.translation.DeepLTranslationProvider
import io.flatzen.translation.FailoverTranslationService
import io.flatzen.translation.TranslationApiKeys
import io.flatzen.translation.TranslationCache
import io.flatzen.translation.TranslationProvider
import io.flatzen.translation.TranslationQuotaStore
import io.flatzen.translation.TranslationService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val TRANSLATION_HTTP = "translationHttpClient"

val translationModule = module {
    single(named(TRANSLATION_HTTP)) {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }
        }
    }

    single { TranslationQuotaStore(TranslationQuotaStore.defaultOrder()) }
    single { TranslationCache() }

    single<TranslationProvider>(named("azure")) {
        AzureTranslationProvider(
            httpClient = get(named(TRANSLATION_HTTP)),
            quotaStore = get(),
        )
    }
    single<TranslationProvider>(named("deepl")) {
        DeepLTranslationProvider(
            httpClient = get(named(TRANSLATION_HTTP)),
            quotaStore = get(),
        )
    }

    single<TranslationService> {
        // Unconfigured providers (empty Azure key) are filtered out inside the service.
        val providers = buildList {
            if (TranslationApiKeys.isAzureConfigured) add(get<TranslationProvider>(named("azure")))
            if (TranslationApiKeys.isDeepLConfigured) add(get<TranslationProvider>(named("deepl")))
        }
        FailoverTranslationService(
            providers = providers,
            quotaStore = get(),
            cache = get(),
        )
    }
}
