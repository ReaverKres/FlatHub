package io.flatzen.commoncomponents.location

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

/**
 * iOS: prefer locale region. Cellular ISO needs CoreTelephony entitlement/link;
 * null/empty falls back to BY in [CountryCode.fromNetworkIso].
 */
actual fun networkCountryIso(): String? {
    val fromLocale = NSLocale.currentLocale.countryCode?.trim().orEmpty()
    return fromLocale.takeIf { it.isNotEmpty() }?.uppercase()
}
