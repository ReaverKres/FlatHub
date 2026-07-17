package listing.th

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Sale vs rent for Thailand residential filters. */
internal fun AdType.isThSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}
