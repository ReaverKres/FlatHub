package io.flatzen.data.di

import di.dataModule
import org.koin.core.context.startKoin
import io.flatzen.viewmodel.FlatSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import repository.KufarRepository

val flatSearchPresentationModule = module {
    viewModel { FlatSearchViewModel(get()) }   // get() -> KufarRepository
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
