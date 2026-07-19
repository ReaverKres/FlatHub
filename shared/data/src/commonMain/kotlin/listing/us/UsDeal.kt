package listing.us

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType

/** Sale vs rent for US residential filters. */
internal fun AdType.isUsSaleDeal(): Boolean = when (this) {
    is AdType.SALE -> true
    is AdType.COMMERCIAL -> commercialAdType == CommercialAdType.SALE
    else -> false
}
