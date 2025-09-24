package io.flatzen.di

import di.dataModule
import di.networkModule
import di.databaseModule
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.FavoritesViewModel
import io.flatzen.viewmodel.MapViewModel
import io.flatzen.viewmodel.SplashScreenViewModel
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.list.FlatSearchViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val flatSearchPresentationModule = module {
    single {
        FlatSearchViewModel(
            mergedRepository = get(),
            filterRepository = get(),
            userPreferencesRepository = get(),
            connectionMonitor = get(),
            analyticsManager = get(),
            configFieldsChecker = get()
        )
    }
    viewModel {
        FlatDetailViewModel(
            mergedRepository = get(),
            tileStreamProvider = get(),
            analyticsManager = get()
        )
    }
    viewModel {
        FavoritesViewModel(
            mergedRepository = get(),
        )
    }
    viewModel {
        FilterViewModel(
            filterRepository = get(),
            analyticsManager = get()
        )
    }

    viewModel { MapViewModel(tileStreamProvider = get()) }

    viewModel {
        SplashScreenViewModel(configManager = get())
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            flatSearchPresentationModule,
            networkModule,
            dataModule,
            firebaseModule(),
            databaseModule(),
            dataUtilsModule(),
            analyticsModule()
        )
    }
}