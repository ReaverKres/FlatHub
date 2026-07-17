package io.flatzen.commoncomponents.location

/**
 * ISO 3166-1 alpha-2 from the cellular network (e.g. "PL", "BY"), or null if unavailable.
 */
expect fun networkCountryIso(): String?
