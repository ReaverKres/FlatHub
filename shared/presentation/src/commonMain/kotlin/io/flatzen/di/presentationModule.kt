package io.flatzen.di

import di.dataModule
import di.networkModule
import di.databaseModule
import io.flatzen.viewmodel.FilterViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.FlatSearchViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val flatSearchPresentationModule = module {
    viewModel { FlatSearchViewModel(
        mergedRepository = get(),
        filterRepository = get(),
        connectionMonitor = get()
    ) }
    viewModel { FlatDetailViewModel(
        mergedRepository = get(),
    ) }
    viewModel { FilterViewModel(
        filterRepository = get(),
        mergedRepository = get()
    ) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            flatSearchPresentationModule,
            networkModule,
            dataModule,
            databaseModule(),
            dataUtilsModule()
        )
    }
}
