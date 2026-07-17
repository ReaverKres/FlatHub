package listing.ae

import io.flatzen.commoncomponents.commonentities.CommercialPropertyType

/**
 * Maps Flatzen [CommercialPropertyType] ↔ Property Finder / Dubizzle / OpenSooq wire values.
 * See tmp/ae/probe_ae_commercial_types*.mjs.
 */
object AeCommercialTypes {
    /** Property Finder `t=` within commercial `c=3|4`. Null = all types. */
    fun propertyFinderTypeId(type: CommercialPropertyType?): Int? = when (type) {
        null, CommercialPropertyType.All -> null
        CommercialPropertyType.Office -> 4
        CommercialPropertyType.Retail -> 21 // Shop (Retail Space = 27 also exists)
        CommercialPropertyType.Warehouses -> 13
        CommercialPropertyType.Showroom -> 12
        CommercialPropertyType.Land -> 5
        CommercialPropertyType.Industrial -> 11 // Labor Camp; Factory = 44
        CommercialPropertyType.Other -> 10 // Whole Building as catch-all “other-ish”
        CommercialPropertyType.Services -> null // not used on AE UI
    }

    /** Dubizzle Algolia leaf `categories.ids`. Null = all commercial in index. */
    fun dubizzleCategoryId(type: CommercialPropertyType?, isSale: Boolean): Int? = when (type) {
        null, CommercialPropertyType.All -> null
        CommercialPropertyType.Office -> if (isSale) 131 else 127
        CommercialPropertyType.Retail -> if (isSale) 2774 else 2773 // shop
        CommercialPropertyType.Warehouses -> if (isSale) 2778 else 2777
        CommercialPropertyType.Showroom -> if (isSale) 2776 else 2775
        CommercialPropertyType.Industrial -> if (isSale) 2780 else 128 // factory / industrial
        CommercialPropertyType.Other -> if (isSale) 2782 else 2781
        CommercialPropertyType.Land -> null // no dedicated Dubizzle leaf; PF/OpenSooq cover land
        CommercialPropertyType.Services -> null
    }

    /** OpenSooq path segment under `/property/`. */
    fun openSooqKind(type: CommercialPropertyType?, isSale: Boolean): String {
        val sale = isSale
        return when (type) {
            null, CommercialPropertyType.All ->
                if (sale) "commercial-for-sale" else "commercial-for-rent"

            CommercialPropertyType.Office ->
                if (sale) "offices-for-sale" else "offices-for-rent"

            CommercialPropertyType.Retail ->
                if (sale) "shops-for-sale" else "shops-for-rent"

            CommercialPropertyType.Warehouses ->
                if (sale) "warehouses-for-sale" else "warehouses-for-rent"

            CommercialPropertyType.Showroom ->
                if (sale) "showrooms-for-sale" else "showrooms-for-rent"

            CommercialPropertyType.Land ->
                if (sale) "land-for-sale" else "land-for-rent"

            CommercialPropertyType.Industrial ->
                if (sale) "factories-for-sale" else "factories-for-rent"

            CommercialPropertyType.Other, CommercialPropertyType.Services ->
                if (sale) "commercial-for-sale" else "commercial-for-rent"
        }
    }

    fun fromPropertyFinderLabel(raw: String?): CommercialPropertyType? {
        if (raw.isNullOrBlank()) return null
        val s = raw.lowercase()
        return when {
            s.contains("office") -> CommercialPropertyType.Office
            s.contains("shop") || s.contains("retail") -> CommercialPropertyType.Retail
            s.contains("warehouse") -> CommercialPropertyType.Warehouses
            s.contains("show") -> CommercialPropertyType.Showroom
            s.contains("land") || s.contains("plot") -> CommercialPropertyType.Land
            s.contains("labor") || s.contains("factory") || s.contains("staff") ||
                    s.contains("industrial") -> CommercialPropertyType.Industrial

            else -> CommercialPropertyType.Other
        }
    }

    fun fromDubizzleSlug(slug: String?): CommercialPropertyType? {
        if (slug.isNullOrBlank()) return null
        return when (slug.lowercase()) {
            "office" -> CommercialPropertyType.Office
            "shop", "retail" -> CommercialPropertyType.Retail
            "warehouse" -> CommercialPropertyType.Warehouses
            "showroom" -> CommercialPropertyType.Showroom
            "industrial", "factory", "staff-accomm" -> CommercialPropertyType.Industrial
            "other", "commercial-villa", "commercial-floor" -> CommercialPropertyType.Other
            else -> CommercialPropertyType.Other
        }
    }

    fun fromOpenSooqCat2(uriOrLabel: String?): CommercialPropertyType? {
        if (uriOrLabel.isNullOrBlank()) return null
        val s = uriOrLabel.lowercase()
        return when {
            s.contains("office") -> CommercialPropertyType.Office
            s.contains("shop") || s.contains("retail") || s.contains("supermarket") ->
                CommercialPropertyType.Retail

            s.contains("warehouse") -> CommercialPropertyType.Warehouses
            s.contains("showroom") -> CommercialPropertyType.Showroom
            s.contains("land") -> CommercialPropertyType.Land
            s.contains("factor") || s.contains("labor") || s.contains("industrial") ->
                CommercialPropertyType.Industrial

            else -> CommercialPropertyType.Other
        }
    }
}
