package io.flatzen.kmpapp

import DetailScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.Serializable


@Serializable
object ListScreenDestination

@Serializable
object FavoritesScreenDestination

@Serializable
object SettingsScreenDestination

@Serializable
data class MapScreenDestination(val selectedMarker: Long? = null)

@Serializable
data class DetailScreenDestination(val flatPlatform: FlatPlatform, val objectId: Long)

@Serializable
object FilterScreenDestination

@Serializable
object LocationScreenDestination

@Serializable
object CitySelectScreenDestination

@Serializable
object MetroSelectScreenDestination

// Определяем элементы для BottomBar
val bottomNavItems = listOf(
    BottomNavItem(ListScreenDestination, "Главная", Icons.Default.Home),
    BottomNavItem(FavoritesScreenDestination, "Избранное", Icons.Default.Favorite),
    BottomNavItem(MapScreenDestination(), "Карта", Icons.Default.LocationOn),
    BottomNavItem(SettingsScreenDestination, "Настройки", Icons.Default.Settings)
)
data class BottomNavItem(val route: Any, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Определяем, является ли текущий маршрут одним из главных экранов.
        val showBottomBar = currentDestination?.route?.let { route ->
            // Показываем BottomBar только на основных экранах вкладок
            route == ListScreenDestination::class.qualifiedName ||
            route == FavoritesScreenDestination::class.qualifiedName ||
            route == SettingsScreenDestination::class.qualifiedName ||
            route.startsWith(MapScreenDestination::class.qualifiedName!!)
        } ?: false

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val isSelected = when (item.route) {
                                ListScreenDestination -> currentDestination?.route == ListScreenDestination::class.qualifiedName
                                FavoritesScreenDestination -> currentDestination?.route == FavoritesScreenDestination::class.qualifiedName
                                SettingsScreenDestination -> currentDestination?.route == SettingsScreenDestination::class.qualifiedName
                                is MapScreenDestination -> currentDestination?.route?.startsWith(MapScreenDestination::class.qualifiedName!!) == true
                                else -> false
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    when (item.route) {
                                        ListScreenDestination -> {
                                            navController.navigate(ListScreenDestination) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        FavoritesScreenDestination -> {
                                            navController.navigate(FavoritesScreenDestination) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        SettingsScreenDestination -> {
                                            navController.navigate(SettingsScreenDestination) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        is MapScreenDestination -> {
                                            navController.navigate(MapScreenDestination()) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
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
            NavHost(
                navController = navController,
                startDestination = ListScreenDestination, // Начинаем с конкретного экрана
                modifier = Modifier.padding(innerPadding)
            ) {
                // Экран списка (основной)
                composable<ListScreenDestination> {
                    io.flatzen.screens.list.ListScreen(
                        navigateToDetails = { platform, id ->
                            navController.navigate(DetailScreenDestination(platform, id))
                        },
                        navigateToFilters = {
                            navController.navigate(FilterScreenDestination)
                        }
                    )
                }
                
                // Экран избранного
                composable<FavoritesScreenDestination> {
                    io.flatzen.screens.favorites.FavoritesScreen(
                        navigateToDetails = { platform, id ->
                            navController.navigate(DetailScreenDestination(platform, id))
                        }
                    )
                }
                
                // Экран настроек
                composable<SettingsScreenDestination> {
                    io.flatzen.screens.settings.SettingsScreen()
                }
                
                // Экран карты
                composable<MapScreenDestination> { backStackEntry ->
                    val args = backStackEntry.toRoute<MapScreenDestination>()
                    io.flatzen.screens.map.MapScreen(
                        selectedMarker = args.selectedMarker,
                        navigateToDetails = { platform, id ->
                            navController.navigate(DetailScreenDestination(platform, id))
                        },
                        navigateToFilters = {
                            navController.navigate(FilterScreenDestination)
                        },
                        navigateBackToDetail = {
                            navController.navigate(ListScreenDestination) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                
                // DetailScreen (общий для всех)
                composable<DetailScreenDestination> { backStackEntry ->
                    val args = backStackEntry.toRoute<DetailScreenDestination>()
                    DetailScreen(
                        flatPlatform = args.flatPlatform,
                        objectId = args.objectId,
                        navigateBack = { navController.popBackStack() },
                        navigateToMap = { flatId ->
                            // Навигация к карте с выбранным маркером
                            navController.navigate(MapScreenDestination(selectedMarker = flatId)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // Общие экраны (фильтры, локация и т.д.)
                // Экран Фильтров (открывается поверх)
                composable<FilterScreenDestination> {
                    io.flatzen.screens.filter.FilterScreen(
                        navigateBack = { navController.popBackStack() },
                        onOpenLocation = { navController.navigate(LocationScreenDestination) }
                    )
                }

                composable<LocationScreenDestination> {
                    io.flatzen.screens.location.LocationScreen(
                        navigateBack = { navController.popBackStack() },
                        openCity = { navController.navigate(CitySelectScreenDestination) },
                        openMetro = { navController.navigate(MetroSelectScreenDestination) }
                    )
                }

                composable<CitySelectScreenDestination> {
                    io.flatzen.screens.location.CitySelectScreen(
                        navigateBack = { navController.popBackStack() }
                    )
                }

                composable<MetroSelectScreenDestination> {
                    io.flatzen.screens.location.MetroSelectScreen(
                        navigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}