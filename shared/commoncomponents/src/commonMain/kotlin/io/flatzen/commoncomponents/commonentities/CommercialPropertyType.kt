package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class CommercialPropertyType {
    companion object {
        /** Belarus Kufar/Realt-oriented set (no All — default is Office in UI). */
        val byInstances: List<CommercialPropertyType> = listOf(
            Office,
            Retail,
            Services,
            Industrial,
            Warehouses,
            Other,
        )

        /**
         * UAE commercial subtypes. Includes [All] (default) plus PF/Dubizzle/OpenSooq types.
         * [Services] omitted — no clean UAE equivalent.
         */
        val aeInstances: List<CommercialPropertyType> = listOf(
            All,
            Office,
            Retail,
            Warehouses,
            Showroom,
            Land,
            Industrial,
            Other,
        )

        /** Backward-compatible alias used by BY UI paths. */
        val allInstances: List<CommercialPropertyType> = byInstances

        fun instancesFor(country: CountryCode): List<CommercialPropertyType> = when (country) {
            CountryCode.AE -> aeInstances
            else -> byInstances
        }

        fun defaultFor(country: CountryCode): CommercialPropertyType = when (country) {
            CountryCode.AE -> All
            else -> Office
        }
    }

    @Serializable
    object All : CommercialPropertyType()

    @Serializable
    object Office : CommercialPropertyType()

    @Serializable
    object Retail : CommercialPropertyType()

    @Serializable
    object Services : CommercialPropertyType()

    @Serializable
    object Industrial : CommercialPropertyType()

    @Serializable
    object Warehouses : CommercialPropertyType()

    @Serializable
    object Other : CommercialPropertyType()

    /** UAE — commercial land plots. */
    @Serializable
    object Land : CommercialPropertyType()

    /** UAE — showrooms. */
    @Serializable
    object Showroom : CommercialPropertyType()
}
