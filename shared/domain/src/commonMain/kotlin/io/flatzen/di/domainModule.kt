package io.flatzen.di

import io.flatzen.usecases.RegistrationUseCase
import org.koin.dsl.module

val domainModule = module {
    single {
        RegistrationUseCase(
            devicePlatform = get(),
            subscriptionsRepository = get(),
            userPreferences = get()
        )
    }
}