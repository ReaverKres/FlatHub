package listing.ca

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Residential [AdType.SALE] or commercial [CommercialAdType.SALE]. */
internal fun AdType.isCaSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}

/** Stable numeric id from external string ad id (composite PK with platform). */
internal fun stableCaAdId(externalId: String): Long =
    externalId.hashCode().toLong() and 0x7FFF_FFFFL
