package io.flatzen.di

import di.dataModule
import di.networkModule
import io.flatzen.viewmodel.FilterViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.FlatSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val flatSearchPresentationModule = module {
    viewModel { FlatSearchViewModel(
        kufarRepository = get(),
        onlinerRepository = get(),
        realtRepository = get(),
        filterRepository = get()
    ) }
    viewModel { FlatDetailViewModel(
        kufarRepository = get(),
        onlinerRepository = get(),
        realtRepository = get()
    ) }
    viewModel { FilterViewModel(
        filterRepository = get(),
        kufarRepository = get(),
        onlinerRepository = get(),
        realtRepository = get()
    ) }
}

fun initKoin() {
    startKoin {
        modules(
            flatSearchPresentationModule,
            networkModule,
            dataModule
        )
    }
}
