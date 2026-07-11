package io.flatzen.navigation

import DetailScreen
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.notifications.NotificationsService
import io.flatzen.screens.favorites.FavoritesScreen
import io.flatzen.screens.filter.FilterScreen
import io.flatzen.screens.home.HomeScreen
import io.flatzen.screens.home.NotificationsScreen
import io.flatzen.screens.location.CitySelectScreen
import io.flatzen.screens.location.DistrictSelectScreen
import io.flatzen.screens.location.LocationScreen
import io.flatzen.screens.location.MetroSelectScreen
import io.flatzen.screens.map.MapScreen
import io.flatzen.screens.more.FaqScreen
import io.flatzen.screens.more.MoreScreen
import io.flatzen.screens.more.ReferralScreen
import io.flatzen.widgets.MessageSnackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

private data class BottomNavItem(val route: Route, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Route.List, "Главная", Icons.Default.Home),
    BottomNavItem(Route.Favorites, "Избранное", Icons.Default.Favorite),
    BottomNavItem(Route.Map(), "Карта", Icons.Default.LocationOn),
    BottomNavItem(Route.Settings, "Ещё", Icons.Default.Menu)
)

@Composable
fun MainGraphNavigation(
    modifier: Modifier = Modifier,
    onExitApp: () -> Unit,
) {
    val connectionMonitor: ConnectionMonitor = koinInject()
    var isConnected by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        connectionMonitor.isNetworkAvailable.collect { connected ->
            isConnected = connected
        }
    }

    val density = LocalDensity.current
    val notificationsService: NotificationsService = koinInject()
    val topLevelRoutes = remember { setOf(Route.List, Route.Favorites, Route.Map(), Route.Settings) }
    val navigationState = rememberNavigationState(
        startRoute = Route.List,
        topLevelRoutes = topLevelRoutes
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val flatHubCommands = rememberFlatHubCommands()

    LaunchedEffect(Unit) {
        notificationsService.notificationClickListener.collect { clickOnNotification ->
            if (clickOnNotification == null) return@collect
            navigator.navigateFromExternal(Route.Notifications())
            notificationsService.notificationClickListener.emit(null)
        }
    }

    LaunchedEffect(Unit) {
        DeepLinkRouter.deepLinks.collect { uri ->
            DeepLinkParser.parse(uri)?.let { route ->
                navigator.navigateFromExternal(route)
            }
        }
    }

    LaunchedEffect(flatHubCommands) {
        flatHubCommands?.collectLatest { command ->
            handleFlatHubCommand(command, navigator)
        }
    }

    val currentTopLevelRoute = navigationState.topLevelRoute
    val currentStack = navigationState.backStacks[currentTopLevelRoute]
    val currentRoute = currentStack?.lastOrNull()
    val isRootRoute = currentRoute == currentTopLevelRoute
    val showBottomBar = currentTopLevelRoute in topLevelRoutes && isRootRoute

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = when (item.route) {
                            Route.List -> currentTopLevelRoute == Route.List
                            Route.Favorites -> currentTopLevelRoute == Route.Favorites
                            Route.Settings -> currentTopLevelRoute == Route.Settings
                            is Route.Map -> currentTopLevelRoute is Route.Map
                            else -> false
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentTopLevelRoute != item.route) {
                                    navigator.navigate(item.route)
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val focusManager = LocalFocusManager.current
        var snackbarHeight by remember { mutableStateOf(0.dp) }
        val additionalTopPadding = if (!isConnected) snackbarHeight else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            val entryProvider = entryProvider<Route> {
                entry<Route.List> {
                    HomeScreen(
                        navigateToDetails = { platform, id ->
                            navigator.navigate(Route.Detail(platform.name, id))
                        },
                        navigateToFilters = { navigator.navigate(Route.Filter) },
                        navigateToNotifications = { navigator.navigate(Route.Notifications()) }
                    )
                }
                entry<Route.Favorites> {
                    FavoritesScreen(
                        navigateToDetails = { platform, id ->
                            navigator.navigate(Route.Detail(platform.name, id))
                        }
                    )
                }
                entry<Route.Settings> {
                    MoreScreen(
                        navigateToFaq = { navigator.navigate(Route.Faq) },
                        navigateToReferral = { navigator.navigate(Route.Referral) }
                    )
                }
                entry<Route.Map> { key ->
                    MapScreen(
                        selectedMarker = key.selectedMarker,
                        navigateToDetails = { platform, id ->
                            navigator.navigate(Route.Detail(platform.name, id))
                        },
                        navigateToFilters = { navigator.navigate(Route.Filter) },
                        navigateBack = { navigator.goBack() }
                    )
                }
                entry<Route.Detail> { key ->
                    val platform = FlatPlatform.entries.firstOrNull {
                        it.name == key.flatPlatform || it.value == key.flatPlatform
                    } ?: error("Unknown flatPlatform: ${key.flatPlatform}")
                    DetailScreen(
                        flatPlatform = platform,
                        objectId = key.objectId,
                        navigateBack = { navigator.goBack() },
                        navigateToMap = { flatId ->
                            navigator.navigate(Route.Map(selectedMarker = flatId))
                        }
                    )
                }
                entry<Route.Filter> {
                    FilterScreen(
                        navigateBack = { navigator.goBack() },
                        onOpenLocation = { navigator.navigate(Route.Location) },
                        onOpenReferralScreen = { navigator.navigate(Route.Referral) }
                    )
                }
                entry<Route.Location> {
                    LocationScreen(
                        navigateBack = { navigator.goBack() },
                        openCity = { navigator.navigate(Route.CitySelect) },
                        openMetro = { navigator.navigate(Route.MetroSelect) },
                        openDistricts = { navigator.navigate(Route.DistrictSelect) }
                    )
                }
                entry<Route.CitySelect> {
                    CitySelectScreen(navigateBack = { navigator.goBack() })
                }
                entry<Route.MetroSelect> {
                    MetroSelectScreen(navigateBack = { navigator.goBack() })
                }
                entry<Route.DistrictSelect> {
                    DistrictSelectScreen(navigateBack = { navigator.goBack() })
                }
                entry<Route.Faq> {
                    FaqScreen(navigateBack = { navigator.goBack() })
                }
                entry<Route.Referral> {
                    ReferralScreen(navigateBack = { navigator.goBack() })
                }
                entry<Route.Notifications> { key ->
                    NotificationsScreen(
                        navigateBack = { navigator.goBack() },
                        navigateToDetails = { platform, id ->
                            navigator.navigate(Route.Detail(platform.name, id))
                        },
                        filterFromNotification = key.filterInNotification
                    )
                }
            }

            @Suppress("UNCHECKED_CAST")
            val provider =
                entryProvider as (NavKey) -> androidx.navigation3.runtime.NavEntry<NavKey>
            AppNavDisplay(
                entries = navigationState.toEntries(provider),
                onBack = {
                    if (navigator.isAtRootOfStartRoute()) {
                        onExitApp()
                    } else {
                        navigator.goBack()
                    }
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(top = additionalTopPadding)
            )
        }

        if (isConnected.not()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .animateContentSize()
                    .onSizeChanged { layout ->
                        snackbarHeight = with(density) { layout.height.toDp() }
                    }
            ) {
                MessageSnackbar(message = "Нет подключения к интернету")
            }
        }
    }
}

@Composable
private fun rememberFlatHubCommands(): Flow<FlatHubCommand>? {
    val delegate: FlatHubNavigatorDelegate = koinInject()
    var commands by remember { mutableStateOf<Flow<FlatHubCommand>?>(null) }

    DisposableEffect(delegate) {
        val emitter = ChannelNavigationEmitter<FlatHubCommand>()
        delegate.attach(emitter)
        commands = emitter.commands
        onDispose {
            delegate.detach()
            commands = null
        }
    }

    return commands
}

private fun handleFlatHubCommand(command: FlatHubCommand, navigator: Navigator) {
    when (command) {
        is FlatHubCommand.OpenDetail ->
            navigator.navigate(Route.Detail(command.platform.name, command.objectId))
        FlatHubCommand.OpenFilter -> navigator.navigate(Route.Filter)
        is FlatHubCommand.OpenNotifications ->
            navigator.navigate(Route.Notifications(command.filterJson))
        is FlatHubCommand.OpenMap ->
            navigator.navigate(Route.Map(selectedMarker = command.selectedMarker))
        FlatHubCommand.OpenFaq -> navigator.navigate(Route.Faq)
        FlatHubCommand.OpenReferral -> navigator.navigate(Route.Referral)
        FlatHubCommand.OpenLocation -> navigator.navigate(Route.Location)
        FlatHubCommand.OpenCitySelect -> navigator.navigate(Route.CitySelect)
        FlatHubCommand.OpenMetroSelect -> navigator.navigate(Route.MetroSelect)
        FlatHubCommand.OpenDistrictSelect -> navigator.navigate(Route.DistrictSelect)
        FlatHubCommand.NavigateBack -> navigator.goBack()
    }
}
