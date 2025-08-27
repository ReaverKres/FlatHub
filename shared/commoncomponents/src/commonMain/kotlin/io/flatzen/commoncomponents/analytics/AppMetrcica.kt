package io.flatzen.commoncomponents.analytics

/**
 * Constants for analytics events and parameters.
 * Centralized location for all analytics event names and parameter keys.
 */
object AppMetrcica {
    
    // Event Names
    object Events {
        const val APP_LAUNCH = "app_launch"
        const val SCREEN_VIEW = "screen_view"
        const val SEARCH_FLATS = "search_flats"
        const val FLAT_FAVORITE_TOGGLE = "flat_favorite_toggle"
        const val FILTER_APPLIED = "filter_applied"
    }
    
    // Parameter Keys
    object Parameters {
        const val SCREEN_NAME = "screen_name"
        const val SCREEN_TYPE = "screen_type"
        const val TIMESTAMP = "timestamp"
        const val APP_VERSION = "app_version"
        const val VERSION_CODE = "version_code"
        const val IS_LOAD_MORE = "is_load_more"
        const val IS_REFRESHING = "is_refreshing"
        const val PAGE = "page"
        const val HAS_NETWORK = "has_network"
        const val FLAT_PLATFORM = "platform"
        const val OBJECT_ID = "object_id"
    }
    
    // Screen Names
    object Screens {
        const val LIST = "list_screen"
        const val DETAIL = "detail_screen"
        const val FILTER = "filter_screen"
        const val FAVORITES = "favorites_screen"
        const val MAP = "map_screen"
        const val SETTINGS = "settings_screen"
    }
    
    // Screen Types
    object ScreenTypes {
        const val MAIN = "main"
        const val MODAL = "modal"
        const val DETAIL = "detail"
    }
}