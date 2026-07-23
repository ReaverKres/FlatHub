package io.flatzen.viewmodel.list

import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.localization.LocalizationKeys
import kotlin.concurrent.Volatile

/**
 * In-memory session store for "don't show this search error again".
 * Cleared on process death; a different error fingerprint shows the dialog again.
 */
object SearchNetworkErrorDismissStore {
    @Volatile
    private var suppressedFingerprint: String? = null

    fun isSuppressed(fingerprint: String): Boolean =
        fingerprint.isNotEmpty() && fingerprint == suppressedFingerprint

    fun suppress(fingerprint: String) {
        if (fingerprint.isNotEmpty()) {
            suppressedFingerprint = fingerprint
        }
    }

    fun fingerprint(
        generalError: LocalizationKeys?,
        platformErrors: List<Pair<FlatPlatform, List<String>>>,
    ): String = buildString {
        append(generalError?.name.orEmpty())
        platformErrors
            .sortedBy { it.first.name }
            .forEach { (platform, messages) ->
                append('|')
                append(platform.name)
                append(':')
                append(messages.joinToString(";"))
            }
    }
}
