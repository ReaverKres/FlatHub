package io.flatzen.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * All navigation routes in the app.
 * Sealed interface for type-safe navigation and polymorphic serialization (KMP).
 */
@Serializable
sealed interface Route : NavKey {

    // Bottom tab routes
    @Serializable
    data object List : Route

    @Serializable
    data object Favorites : Route

    @Serializable
    data object Swipe : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class Map(
        val selectedMarker: Long? = null,
        val selectedLatitude: Double? = null,
        val selectedLongitude: Double? = null,
        val selectedRooms: Int? = null,
    ) : Route

    // Detail
    @Serializable
    data class Detail(
        val flatPlatform: String,
        val objectId: Long,
        val markAsViewedOnOpen: Boolean = true,
    ) : Route

    // Modals / nested screens
    @Serializable
    data object Filter : Route

    @Serializable
    data object Location : Route

    @Serializable
    data object CitySelect : Route

    @Serializable
    data object MetroSelect : Route

    @Serializable
    data object DistrictSelect : Route

    @Serializable
    data object Faq : Route

    @Serializable
    data object Language : Route

    @Serializable
    data object Referral : Route

    @Serializable
    data object Premium : Route

    @Serializable
    data class Notifications(val filterInNotification: String? = null) : Route
}
