package listing.fr

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Residential [AdType.SALE] or commercial [CommercialAdType.SALE]. */
internal fun AdType.isFrSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}

/** Stable numeric id from Bien'ici string ad id (composite PK with [FlatPlatform.BIENICI]). */
internal fun stableFrAdId(externalId: String): Long =
    externalId.hashCode().toLong() and 0x7FFF_FFFFL
