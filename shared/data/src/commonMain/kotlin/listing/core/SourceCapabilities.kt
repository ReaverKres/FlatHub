package listing.core

import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType

data class SourceCapabilities(
    val supportsRent: Boolean = true,
    val supportsSale: Boolean = true,
    val supportsDaily: Boolean = false,
    val supportsRoom: Boolean = false,
    val supportsCommercial: Boolean = false,
) {
    fun matches(filter: CommonFilterRequestModel): Boolean {
        if (filter.isRoomForRent) return supportsRoom
        if (filter.isCommercial) return supportsCommercial
        return when (filter.adType) {
            is AdType.RENT -> supportsRent
            is AdType.SALE -> supportsSale
            is AdType.DAILY -> supportsDaily
            is AdType.COMMERCIAL -> supportsCommercial
            else -> supportsRent
        }
    }
}
