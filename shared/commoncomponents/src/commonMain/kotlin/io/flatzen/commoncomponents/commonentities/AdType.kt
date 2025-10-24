package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class AdType {
    @Serializable
    data object RENT: AdType()
    @Serializable
    data object SALE: AdType()
    @Serializable
    data class COMMERCIAL(val commercialAdType: CommercialAdType = CommercialAdType.RENT): AdType()
}

val AdType.isCommercial: Boolean
    get() = this == AdType.COMMERCIAL(
        CommercialAdType.RENT
    ) || this == AdType.COMMERCIAL(
        CommercialAdType.SALE
    )

enum class CommercialAdType {
    RENT, SALE
}