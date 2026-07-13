package io.flatzen.di

import dev.icerock.moko.permissions.PermissionsController
import di.dataModule
import di.databaseModule
import di.networkModule
import io.flatzen.analytics.di.analyticsModule
import io.flatzen.monetization.di.monetizationModules
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.navigation.FlatHubNavigatorDelegate
import io.flatzen.viewmodel.DistrictsContainer
import io.flatzen.viewmodel.FavoritesContainer
import io.flatzen.viewmodel.MapContainer
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.detailad.FlatDetailContainer
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.list.FlatSearchContainer
import io.flatzen.viewmodel.more.FaqContainer
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.viewmodel.more.ReferralContainer
import io.flatzen.viewmodel.notifications.NotificationListContainer
import io.flatzen.viewmodel.notifications.ToggleNotificationsContainer
import io.flatzen.viewmodel.premium.PremiumContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.new
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val flatSearchPresentationModule = module {
    single { FlatHubNavigatorDelegate() }
    single<FlatHubNavigator> { get<FlatHubNavigatorDelegate>() }
    single {
        FlatSearchContainer(
            mergedRepository = get(),
            filterRepository = get(),
            userPreferencesRepository = get(),
            connectionMonitor = get(),
            analytics = get(),
            configFieldsChecker = get(),
            userTierProvider = get(),
            navigator = get(),
        ).apply {
            store.start(CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()))
        }
    }
    container { new(::FlatDetailContainer) }
    container { new(::FavoritesContainer) }

    container { (controller: PermissionsController, filter: String?) ->
        NotificationListContainer(
            mergedRepository = get(),
            userPreferencesRepository = get(),
            subscriptionsRepository = get(),
            permissionsController = controller,
            devicePlatform = get(),
            filterFromNotification = filter,
            navigator = get(),
        )
    }

    container { new(::FilterContainer) }

    container { new(::DistrictsContainer) }

    container { new(::MapContainer) }

    container { new(::SplashContainer) }
    container { new(::MoreContainer) }
    container { new(::FaqContainer) }

    container { new(::ReferralContainer) }
    container {
        PremiumContainer(
            subscriptionService = get(),
            trustedTimeRepository = get(),
            adService = get(),
            monetizationRemoteConfig = get(),
            navigator = get(),
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
            analyticsModule,
            *monetizationModules().toTypedArray(),
        )
    }
}