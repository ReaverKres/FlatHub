package io.flatzen.di

import dev.icerock.moko.permissions.PermissionsController
import di.dataModule
import di.databaseModule
import di.networkModule
import io.flatzen.viewmodel.DistrictsViewModel
import io.flatzen.viewmodel.FavoritesViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.MapViewModel
import io.flatzen.viewmodel.SplashScreenViewModel
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.more.FaqViewModel
import io.flatzen.viewmodel.more.MoreScreenViewModel
import io.flatzen.viewmodel.more.ReferralViewModel
import io.flatzen.viewmodel.notifications.NotificationListViewModel
import io.flatzen.viewmodel.notifications.ToggleNotificationsViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
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
            filterRepository = get(),
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

    viewModel { NotificationListViewModel(
        mergedRepository = get(),
        userPreferencesRepository = get(),
        subscriptionsRepository = get(),
        devicePlatform = get()
    ) }

    viewModel {
        FilterViewModel(
            filterRepository = get(),
            userMapAreaRepository = get(),
            analyticsManager = get()
        )
    }

    viewModel { DistrictsViewModel(
        osmRepository = get(),
        filterRepository = get()
    ) }

    viewModel {
        MapViewModel(
            tileStreamProvider = get(),
            userMapAreaRepository = get(),
            filterRepository = get()
        )
    }

    viewModel {
        SplashScreenViewModel(configManager = get())
    }

    viewModel {
        MoreScreenViewModel(
            configFieldsChecker = get(),
            userPreferencesRepository = get()
        )
    }

    viewModel {
        FaqViewModel(configFieldsChecker = get())
    }

    viewModel {
        ReferralViewModel(
            referralRepo = get(),
            devicePlatform = get()
        )
    }

    viewModel { (controller: PermissionsController) ->
        ToggleNotificationsViewModel(
            permissionsController = controller,
            notificationsService = get(),
            subscriptionsRepository = get(),
            filterRepository = get(),
            devicePlatform = get()
        )
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
            notificationsModule(),
            databaseModule(),
            dataUtilsModule(),
            analyticsModule()
        )
    }
}