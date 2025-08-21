package io.flatzen.kmpapp

import DetailScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.navigation.navigation
import androidx.navigation.toRoute
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.Serializable

// Главные экраны для BottomBar
@Serializable
object ListGraph

@Serializable
object FavoritesGraph

@Serializable
object SettingsGraph

@Serializable
object MapGraph

// Вложенные экраны
@Serializable
object ListScreenDestination

@Serializable
object FavoritesScreenDestination

@Serializable
object SettingsScreenDestination

@Serializable
object MapScreenDestination

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

// Определяем элементы для BottomBar. Теперь маршрут - это сам объект-назначение.
val bottomNavItems = listOf(
    BottomNavItem(ListGraph, "Список", Icons.Default.List),
    BottomNavItem(FavoritesGraph, "Избранное", Icons.Default.Favorite),
    BottomNavItem(MapGraph, "Карта", Icons.Default.LocationOn),
    BottomNavItem(SettingsGraph, "Настройки", Icons.Default.Settings)
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
        // Это более простой и надежный способ.
        val showBottomBar = currentDestination?.route in listOf(
            ListScreenDestination::class.qualifiedName,
            FavoritesScreenDestination::class.qualifiedName,
            SettingsScreenDestination::class.qualifiedName,
            MapScreenDestination::class.qualifiedName
        )

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any {
                                it.route == item.route::class.qualifiedName
                            } == true

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
                startDestination = ListGraph, // Стартовый граф
                modifier = Modifier.padding(innerPadding)
            ) {
                // Граф для вкладки "Список"
                navigation<ListGraph>(
                    startDestination = ListScreenDestination
                ) {
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
                }

                // Граф для вкладки "Избранное"
                navigation<FavoritesGraph>(
                    startDestination = FavoritesScreenDestination
                ) {
                    composable<FavoritesScreenDestination> {
                        io.flatzen.screens.favorites.FavoritesScreen(
                            navigateToDetails = { platform, id ->
                                navController.navigate(DetailScreenDestination(platform, id))
                            }
                        )
                    }
                }

                // Граф для вкладки "Настройки"
                navigation<SettingsGraph>(
                    startDestination = SettingsScreenDestination
                ) {
                    composable<SettingsScreenDestination> {
                        io.flatzen.screens.settings.SettingsScreen()
                    }
                }

                // Граф для вкладки "Карта"
                navigation<MapGraph>(
                    startDestination = MapScreenDestination
                ) {
                    composable<MapScreenDestination> {
                        io.flatzen.screens.map.MapScreen()
                    }
                }

                // Экран Деталей (открывается поверх)
                composable<DetailScreenDestination> { backStackEntry ->
                    val args = backStackEntry.toRoute<DetailScreenDestination>()
                    DetailScreen( // Укажите полный путь, если необходимо
                        flatPlatform = args.flatPlatform,
                        objectId = args.objectId,
                        navigateBack = { navController.popBackStack() }
                    )
                }

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