package listing.pl

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Residential [AdType.SALE] or commercial [CommercialAdType.SALE]. */
internal fun AdType.isPlSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}
