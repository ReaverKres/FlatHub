package listing.core

import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform

/**
 * Optional remote-config / feature-flag hook.
 * Empty set means "enable all registered sources for the country".
 */
fun interface ListingPlatformConfig {
    fun enabledPlatforms(country: CountryCode): Set<FlatPlatform>
}

object AllListingPlatformsConfig : ListingPlatformConfig {
    override fun enabledPlatforms(country: CountryCode): Set<FlatPlatform> = emptySet()
}

/**
 * Resolves which [ListingSource]s participate in a search.
 */
class ListingSourceRegistry(
    private val sources: List<ListingSource>,
    private val platformConfig: ListingPlatformConfig = AllListingPlatformsConfig,
) {
    fun forFilter(filter: CommonFilterRequestModel): List<ListingSource> {
        val country = filter.location?.country ?: CountryCode.BY
        val enabled = enabledPlatforms(country)
        return sources.filter { source ->
            source.country == country &&
                    source.platform in enabled &&
                    source.capabilities.matches(filter)
        }
    }

    /**
     * OR of [SourceCapabilities] across enabled platforms for [country].
     * Drives which filter chips/switches the UI should expose.
     */
    fun capabilitiesFor(country: CountryCode): SourceCapabilities {
        val enabled = enabledPlatforms(country)
        val countrySources = sources.filter { it.country == country && it.platform in enabled }
        if (countrySources.isEmpty()) {
            return SourceCapabilities(
                supportsRent = true,
                supportsSale = true,
                supportsDaily = false,
                supportsRoom = false,
                supportsCommercial = false,
            )
        }
        return SourceCapabilities(
            supportsRent = countrySources.any { it.capabilities.supportsRent },
            supportsSale = countrySources.any { it.capabilities.supportsSale },
            supportsDaily = countrySources.any { it.capabilities.supportsDaily },
            supportsRoom = countrySources.any { it.capabilities.supportsRoom },
            supportsCommercial = countrySources.any { it.capabilities.supportsCommercial },
        )
    }

    fun byPlatform(platform: FlatPlatform): ListingSource? =
        sources.firstOrNull { it.platform == platform }

    fun all(): List<ListingSource> = sources

    private fun enabledPlatforms(country: CountryCode): Set<FlatPlatform> {
        val fromConfig = platformConfig.enabledPlatforms(country)
        if (fromConfig.isNotEmpty()) return fromConfig
        return sources.filter { it.country == country }.map { it.platform }.toSet()
    }
}
