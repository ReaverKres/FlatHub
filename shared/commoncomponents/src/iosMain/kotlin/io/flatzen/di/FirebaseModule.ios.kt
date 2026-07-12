package io.flatzen.di

import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.firebase.ConfigManager
import io.flatzen.firebase.ConfigManagerImpl
import io.flatzen.firebase.RemoteConfigRepository
import io.flatzen.firebase.RemoteConfigRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun firebaseModule(): Module = module {
    val configManager = ConfigManagerImpl()
    single<ConfigManager> { configManager }
    single<ConfigFieldsChecker> { configManager }
    single<RemoteConfigRepository> {
        RemoteConfigRepositoryImpl(
            configManager = get(),
            configFieldsChecker = get(),
            connectionMonitor = get(),
        )
    }
}