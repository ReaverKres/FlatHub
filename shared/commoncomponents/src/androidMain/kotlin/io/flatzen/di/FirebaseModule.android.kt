package io.flatzen.di

import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.firebase.ConfigManager
import io.flatzen.firebase.ConfigManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun firebaseModule(): Module = module {
    val configManager = ConfigManagerImpl()
    single<ConfigManager> { configManager }
    single<ConfigFieldsChecker> { configManager }
}