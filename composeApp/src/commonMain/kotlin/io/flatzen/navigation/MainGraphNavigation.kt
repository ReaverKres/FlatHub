package io.flatzen.navigation

import DetailScreen
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.no_internet
import flatzen.composeapp.generated.resources.tab_favorites
import flatzen.composeapp.generated.resources.tab_home
import flatzen.composeapp.generated.resources.tab_map
import flatzen.composeapp.generated.resources.tab_more
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.commoncomponents.theme.ThemeMode
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
import io.flatzen.screens.swipe.SwipeScreen
import io.flatzen.themes.LocalThemeRevealController
import io.flatzen.themes.ThemeRevealHost
import io.flatzen.themes.resolveDark
import io.flatzen.widgets.MessageSnackbar
import io.flatzen.widgets.SwipeCardsIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import repository.userpreferences.UserPreferencesRepository

private data class BottomNavItem(
    val route: Route,
    val label: StringResource,
    val icon: ImageVector? = null,
    val useSwipeIcon: Boolean = false,
)

private val bottomNavItems = listOf(
    BottomNavItem(Route.List, Res.string.tab_home, Icons.Default.Home),
    BottomNavItem(Route.Favorites, Res.string.tab_favorites, Icons.Default.Favorite),
    BottomNavItem(Route.Swipe, Res.string.tab_home, useSwipeIcon = true),
    BottomNavItem(Route.Map(), Res.string.tab_map, Icons.Default.LocationOn),
    BottomNavItem(Route.Settings, Res.string.tab_more, Icons.Default.Menu),
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
    val topLevelRoutes = remember {
        setOf(Route.List, Route.Favorites, Route.Swipe, Route.Map(), Route.Settings)
    }
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

    val userPreferences: UserPreferencesRepository = koinInject()
    val themeMode by userPreferences.observeThemeMode()
        .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val systemDark = isSystemInDarkTheme()
    val revealController = LocalThemeRevealController.current

    ThemeRevealHost(
        controller = revealController,
        committedMode = themeMode,
        isDark = themeMode.resolveDark(systemDark),
        onCommit = { mode -> userPreferences.setThemeMode(mode) },
        modifier = modifier,
    ) {
        MainGraphScaffold(
            navigationState = navigationState,
            navigator = navigator,
            topLevelRoutes = topLevelRoutes,
            isConnected = isConnected,
            density = density,
            onExitApp = onExitApp,
        )
    }
}

@Composable
private fun MainGraphScaffold(
    navigationState: NavigationState,
    navigator: Navigator,
    topLevelRoutes: Set<Route>,
    isConnected: Boolean,
    density: Density,
    onExitApp: () -> Unit,
) {
    val currentTopLevelRoute = navigationState.topLevelRoute
    val currentStack = navigationState.backStacks[currentTopLevelRoute]
    val currentRoute = currentStack?.lastOrNull()
    val isRootRoute = currentRoute == currentTopLevelRoute
    val showBottomBar = currentTopLevelRoute in topLevelRoutes && isRootRoute

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = when (item.route) {
                            Route.List -> currentTopLevelRoute == Route.List
                            Route.Favorites -> currentTopLevelRoute == Route.Favorites
                            Route.Swipe -> currentTopLevelRoute == Route.Swipe
                            Route.Settings -> currentTopLevelRoute == Route.Settings
                            is Route.Map -> currentTopLevelRoute is Route.Map
                            else -> false
                        }

                        val label = stringResource(item.label)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentTopLevelRoute != item.route) {
                                    navigator.navigate(item.route)
                                }
                            },
                            icon = {
                                if (item.useSwipeIcon) {
                                    SwipeCardsIcon(
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp),
                                    )
                                } else {
                                    Icon(item.icon!!, contentDescription = label)
                                }
                            },
                            label = null,
                            alwaysShowLabel = false,
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
                entry<Route.Swipe> {
                    SwipeScreen(
                        navigateToDetails = { platform, id ->
                            navigator.navigate(
                                Route.Detail(
                                    flatPlatform = platform.name,
                                    objectId = id,
                                    markAsViewedOnOpen = false,
                                )
                            )
                        },
                        navigateToFilters = { navigator.navigate(Route.Filter) },
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
                        markAsViewedOnOpen = key.markAsViewedOnOpen,
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
                MessageSnackbar(message = stringResource(Res.string.no_internet))
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
