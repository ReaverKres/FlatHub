package listing.core

import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker

/**
 * Reads country-scoped platform kill-switches from Firebase Remote Config.
 *
 * Empty / missing value → empty set → [ListingSourceRegistry] enables all registered
 * sources for that country.
 */
class RemoteListingPlatformConfig(
    private val configFieldsChecker: ConfigFieldsChecker,
) : ListingPlatformConfig {
    override fun enabledPlatforms(country: CountryCode): Set<FlatPlatform> {
        val raw = when (country) {
            CountryCode.PL -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsPl)
            CountryCode.GE -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsGe)
            CountryCode.KZ -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsKz)
            CountryCode.ES -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsEs)
            CountryCode.DE -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsDe)
            CountryCode.TR -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsTr)
            CountryCode.AE -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsAe)
            CountryCode.TH -> configFieldsChecker.checkString(ConfigFields.EnabledPlatformsTh)
            else -> null
        }
        return parsePlatformList(raw)
    }

    companion object {
        fun parsePlatformList(raw: String?): Set<FlatPlatform> {
            if (raw.isNullOrBlank()) return emptySet()
            val trimmed = raw.trim()
            val tokens = if (trimmed.startsWith("[")) {
                trimmed
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(',')
                    .map { it.trim().trim('"').trim('\'') }
            } else {
                trimmed.split(Regex("[,;\\s]+"))
            }
            return tokens
                .mapNotNull { token ->
                    if (token.isBlank()) return@mapNotNull null
                    FlatPlatform.entries.firstOrNull { platform ->
                        platform.name.equals(token, ignoreCase = true) ||
                                platform.value.equals(token, ignoreCase = true)
                    }
                }
                .toSet()
        }
    }
}
