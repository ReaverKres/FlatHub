package listing.ae

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Sale vs rent for residential and commercial AE filters. */
internal fun AdType.isAeSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}
