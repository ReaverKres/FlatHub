package io.flatzen.di

import dev.icerock.moko.permissions.PermissionsController
import di.dataModule
import di.databaseModule
import di.networkModule
import io.flatzen.viewmodel.DistrictsContainer
import io.flatzen.viewmodel.FavoritesContainer
import io.flatzen.viewmodel.MapContainer
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.detailad.FlatDetailContainer
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.list.FlatSearchViewModel
import io.flatzen.viewmodel.more.FaqContainer
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.viewmodel.more.ReferralContainer
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
    container { new(::FlatDetailContainer) }
    container { new(::FavoritesContainer) }

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

    container { new(::DistrictsContainer) }

    container { new(::MapContainer) }

    container { new(::SplashContainer) }
    container { new(::MoreContainer) }
    container { new(::FaqContainer) }

    container { new(::ReferralContainer) }

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