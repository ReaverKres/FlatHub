package io.flatzen.di

import dev.icerock.moko.permissions.PermissionsController
import di.dataModule
import di.databaseModule
import di.networkModule
import io.flatzen.viewmodel.DistrictsViewModel
import io.flatzen.viewmodel.FavoritesViewModel
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.MapViewModel
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.more.FaqContainer
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.viewmodel.more.ReferralViewModel
import io.flatzen.viewmodel.notifications.NotificationListViewModel
import io.flatzen.viewmodel.notifications.ToggleNotificationsContainer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.new
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

    viewModel { (controller: PermissionsController, filter: String?) ->
        NotificationListViewModel(
            mergedRepository = get(),
            userPreferencesRepository = get(),
            subscriptionsRepository = get(),
            permissionsController = controller,
            devicePlatform = get(),
            filterFromNotification = filter
        )
    }

    viewModel {
        FilterViewModel(
            filterRepository = get(),
            userMapAreaRepository = get(),
            analyticsManager = get(),
            userPreferencesRepository = get()
        )
    }

    viewModel {
        DistrictsViewModel(
            osmRepository = get(),
            filterRepository = get()
        )
    }

    viewModel {
        MapViewModel(
            tileStreamProvider = get(),
            userMapAreaRepository = get(),
            filterRepository = get()
        )
    }

    container { new(::SplashContainer) }
    container { new(::MoreContainer) }
    container { new(::FaqContainer) }

    viewModel {
        ReferralViewModel(
            registrationUseCase = get(),
            notificationsService = get(),
            prefsRepo = get(),
            referralRepo = get(),
            devicePlatform = get()
        )
    }

    container { (controller: PermissionsController) ->
        ToggleNotificationsContainer(
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
            domainModule,
            firebaseModule(),
            notificationsModule(),
            databaseModule(),
            dataUtilsModule(),
            analyticsModule()
        )
    }
}