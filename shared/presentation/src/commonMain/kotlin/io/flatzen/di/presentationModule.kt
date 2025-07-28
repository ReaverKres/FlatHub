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
    viewModel { FlatSearchViewModel(get(), get()) }
    viewModel { FlatDetailViewModel(get(), get()) }
    viewModel { FilterViewModel() }
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
