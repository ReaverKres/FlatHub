package io.flatzen.navigation

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

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: return
        val currentRoute = currentStack.lastOrNull() ?: return

        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute == state.startRoute) {
                // At root of start tab - caller should exit app
                return
            }
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /** Replaces current screen without adding to stack */
    fun replaceCurrent(route: NavKey) {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        currentStack.removeLastOrNull()
        currentStack.add(route)
    }

    /** True when Back should exit app (at root of start tab) */
    fun isAtRootOfStartRoute(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return false
        val currentRoute = currentStack.lastOrNull() ?: return false
        return currentRoute == state.topLevelRoute && state.topLevelRoute == state.startRoute
    }

    /** True when Back can pop or switch tab */
    fun canGoBack(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return false
        val currentRoute = currentStack.lastOrNull() ?: return false
        return currentRoute != state.topLevelRoute || state.topLevelRoute != state.startRoute
    }
}
