package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class AdType {
    @Serializable
    object RENT: AdType()
    @Serializable
    object SALE: AdType()
    @Serializable
    data class COMMERCIAL(val commercialAdType: CommercialAdType = CommercialAdType.RENT): AdType()
    @Serializable
    object DAILY: AdType()
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