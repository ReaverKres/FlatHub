package io.flatzen.navigation

import io.flatzen.commoncomponents.commonentities.FlatPlatform

/**
 * Parses deep link URLs to Route.
 * Supported formats:
 * - flatzen://detail/{platform}/{id}
 * - flatzen://notifications?filter=...
 * - flatzen://map?marker={id}
 * - flatzen://filter
 * - flatzen://location
 * - https://flatzen.app/... (universal links)
 */
object DeepLinkParser {

    private const val SCHEME_FLATZEN = "flatzen"
    private const val HOST_FLATZEN = "flatzen.app"

    fun parse(uri: String): Route? {
        return try {
            val normalized = uri.trim()
            when {
                normalized.startsWith("$SCHEME_FLATZEN://") -> parseCustomScheme(normalized)
                normalized.startsWith("https://$HOST_FLATZEN/") -> parseHttps(normalized)
                normalized.startsWith("http://$HOST_FLATZEN/") -> parseHttps(normalized)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCustomScheme(uri: String): Route? {
        val withoutScheme = uri.removePrefix("$SCHEME_FLATZEN://")
        val pathAndQuery = withoutScheme.split("?", limit = 2)
        val path = pathAndQuery[0].trim('/')
        val query = pathAndQuery.getOrNull(1)?.let { parseQuery(it) } ?: emptyMap()

        return when {
            path.startsWith("detail/") -> {
                val parts = path.removePrefix("detail/").split("/")
                if (parts.size >= 2) {
                    val platform = FlatPlatform.entries.firstOrNull { it.name == parts[0] || it.value == parts[0] }
                    val id = parts[1].toLongOrNull()
                    if (platform != null && id != null) {
                        Route.Detail(flatPlatform = platform.name, objectId = id)
                    } else null
                } else null
            }
            path == "notifications" -> Route.Notifications(filterInNotification = query["filter"])
            path == "map" -> Route.Map(selectedMarker = query["marker"]?.toLongOrNull())
            path == "filter" -> Route.Filter
            path == "location" -> Route.Location
            path.isEmpty() || path == "list" -> Route.List
            path == "favorites" -> Route.Favorites
            path == "settings" -> Route.Settings
            else -> null
        }
    }

    private fun parseHttps(uri: String): Route? {
        val withoutScheme = uri.removePrefix("https://").removePrefix("http://")
        val pathAndQuery = withoutScheme.removePrefix("$HOST_FLATZEN/").split("?", limit = 2)
        val path = pathAndQuery[0].trim('/')
        val query = pathAndQuery.getOrNull(1)?.let { parseQuery(it) } ?: emptyMap()

        return when {
            path.startsWith("detail/") -> {
                val parts = path.removePrefix("detail/").split("/")
                if (parts.size >= 2) {
                    val platform = FlatPlatform.entries.firstOrNull { it.name == parts[0] || it.value == parts[0] }
                    val id = parts[1].toLongOrNull()
                    if (platform != null && id != null) {
                        Route.Detail(flatPlatform = platform.name, objectId = id)
                    } else null
                } else null
            }
            path == "notifications" -> Route.Notifications(filterInNotification = query["filter"])
            path == "map" -> Route.Map(selectedMarker = query["marker"]?.toLongOrNull())
            path == "filter" -> Route.Filter
            path == "location" -> Route.Location
            path.isEmpty() || path == "list" -> Route.List
            path == "favorites" -> Route.Favorites
            path == "settings" -> Route.Settings
            else -> null
        }
    }

    private fun parseQuery(queryString: String): Map<String, String> {
        return queryString.split("&").mapNotNull { param ->
            val (key, value) = param.split("=", limit = 2)
            if (key.isNotBlank()) key to (value.orEmpty()) else null
        }.toMap()
    }
}
