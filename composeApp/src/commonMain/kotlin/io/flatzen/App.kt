package io.flatzen.kmpapp

import DetailScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
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

// Вложенные экраны
@Serializable
object ListScreenDestination

@Serializable
object FavoritesScreenDestination

@Serializable
object SettingsScreenDestination

@Serializable
data class DetailScreenDestination(val flatPlatform: FlatPlatform, val objectId: Long)

@Serializable
object FilterScreenDestination
// Определяем элементы для BottomBar. Теперь маршрут - это сам объект-назначение.
val bottomNavItems = listOf(
    BottomNavItem(ListGraph, "Список", Icons.Default.List),
    BottomNavItem(FavoritesGraph, "Избранное", Icons.Default.Favorite),
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
            SettingsScreenDestination::class.qualifiedName
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        navigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}