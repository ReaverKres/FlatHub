package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class CommercialPropertyType {
    companion object {
        val allInstances: List<CommercialPropertyType> = listOf(
//            All,
            Office,
            Retail,
            Services,
            Industrial,
            Warehouses,
            Other
        )
    }
    @Serializable
    object All: CommercialPropertyType()
    @Serializable
    object Office: CommercialPropertyType()
    @Serializable
    object Retail: CommercialPropertyType()
    @Serializable
    object Services: CommercialPropertyType()
    @Serializable
    object Industrial: CommercialPropertyType()
    @Serializable
    object Warehouses: CommercialPropertyType()
    @Serializable
    object Other: CommercialPropertyType()
}