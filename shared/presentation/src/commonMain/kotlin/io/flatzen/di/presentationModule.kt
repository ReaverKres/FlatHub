package io.flatzen.data.di

import di.dataModule
import di.networkModule
import io.flatzen.viewmodel.FlatDetailViewModel
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.FlatSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val flatSearchPresentationModule = module {
    viewModel { FlatSearchViewModel(get()) }
    viewModel { FlatDetailViewModel(get()) }
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
