package listing.ch

import io.flatzen.commoncomponents.commonentities.AdType

internal fun AdType.isChSaleDeal(): Boolean = this is AdType.SALE
