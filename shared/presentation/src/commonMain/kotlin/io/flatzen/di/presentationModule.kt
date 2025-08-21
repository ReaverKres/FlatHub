package io.flatzen.di

import di.dataModule
import di.networkModule
import di.databaseModule
import io.flatzen.viewmodel.FilterViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.FavoritesViewModel
import io.flatzen.viewmodel.MapViewModel
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.FlatSearchViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import maps.CachedOsmTileProvider
import ovh.plrapps.mapcompose.core.TileStreamProvider

val flatSearchPresentationModule = module {
    viewModel { FlatSearchViewModel(
        mergedRepository = get(),
        filterRepository = get(),
        connectionMonitor = get()
    ) }
    viewModel { FlatDetailViewModel(
        mergedRepository = get(),
    ) }
    viewModel { FavoritesViewModel(
        mergedRepository = get(),
    ) }
    viewModel { FilterViewModel(
        filterRepository = get(),
        mergedRepository = get()
    ) }

    viewModel { MapViewModel(tileStreamProvider = get()) }
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
