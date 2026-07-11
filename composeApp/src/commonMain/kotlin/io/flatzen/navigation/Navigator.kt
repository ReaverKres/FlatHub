package io.flatzen.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (navigate, goBack, replaceCurrent).
 */
class Navigator(private val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun getCurrentStack(): NavBackStack<NavKey>? = state.backStacks[state.topLevelRoute]

    fun isAtDestination(route: NavKey): Boolean {
        val current = getCurrentStack()?.lastOrNull() ?: return false
        return if (route in state.backStacks.keys) {
            state.topLevelRoute == route && current == route
        } else {
            current == route
        }
    }

    fun navigateFromExternal(route: NavKey) {
        if (!isAtDestination(route)) navigate(route)
    }

    fun goBack() {
        val currentStack = getCurrentStack() ?: return
        val currentRoute = currentStack.lastOrNull() ?: return

        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute == state.startRoute) {
                return
            }
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    fun replaceCurrent(route: NavKey) {
        val currentStack = getCurrentStack() ?: return
        currentStack.removeLastOrNull()
        currentStack.add(route)
    }

    fun isAtRootOfStartRoute(): Boolean {
        val currentStack = getCurrentStack() ?: return false
        val currentRoute = currentStack.lastOrNull() ?: return false
        return currentRoute == state.topLevelRoute && state.topLevelRoute == state.startRoute
    }

    fun canGoBack(): Boolean {
        val currentStack = getCurrentStack() ?: return false
        val currentRoute = currentStack.lastOrNull() ?: return false
        return currentRoute != state.topLevelRoute || state.topLevelRoute != state.startRoute
    }
}
