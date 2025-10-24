package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class AdType {
    @Serializable
    data object RENT: AdType()
    @Serializable
    data object SALE: AdType()
    @Serializable
    data class COMMERCIAL(val commercialType: CommercialType = CommercialType.RENT): AdType()
}

val AdType.isCommercial: Boolean
    get() = this == AdType.COMMERCIAL(
        CommercialType.RENT
    ) || this == AdType.COMMERCIAL(
        CommercialType.SALE
    )

enum class CommercialType {
    RENT, SALE
}