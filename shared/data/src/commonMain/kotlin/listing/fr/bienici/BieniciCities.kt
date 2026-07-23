package listing.fr.bienici

import io.flatzen.commoncomponents.commonentities.CityCode

/** Bien'ici `locationNames` + public detail URL slugs. */
object BieniciCities {
    fun locationName(city: CityCode?): String = when (city) {
        null, CityCode.PARIS -> "Paris"
        CityCode.LYON -> "Lyon"
        CityCode.MARSEILLE -> "Marseille"
        CityCode.TOULOUSE -> "Toulouse"
        CityCode.NICE -> "Nice"
        CityCode.NANTES -> "Nantes"
        CityCode.BORDEAUX -> "Bordeaux"
        CityCode.LILLE -> "Lille"
        CityCode.STRASBOURG -> "Strasbourg"
        CityCode.MONTPELLIER -> "Montpellier"
        else -> "Paris"
    }

    /**
     * Canonical public URL — SPA route, e.g.
     * `/annonce/location/paris-11e/appartement/2pieces/apimo-5517907`
     * Short `/annonce/location/{id}` opens the search shell, not the listing.
     */
    fun detailUrl(
        externalId: String,
        adTypeFr: String?,
        city: String?,
        postalCode: String?,
        propertyType: String?,
        rooms: Int?,
    ): String {
        val deal = normalizeDeal(adTypeFr)
        val citySlug = citySlug(city, postalCode)
        val propSlug = propertyTypeSlug(propertyType)
        val roomsSlug = rooms?.takeIf { it > 0 }?.let { "${it}pieces" }
        return buildString {
            append(BASE)
            append("/annonce/")
            append(deal)
            append('/')
            append(citySlug)
            append('/')
            append(propSlug)
            if (roomsSlug != null) {
                append('/')
                append(roomsSlug)
            }
            append('/')
            append(externalId)
        }
    }

    fun externalIdFromDetailUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val path = url.substringBefore('?').trimEnd('/')
        val tail = path.substringAfterLast('/')
        if (tail.isNotBlank() && !RESERVED_SEGMENTS.contains(tail.lowercase())) {
            return tail
        }
        val afterAnnonce = path.substringAfter("/annonce/", "")
        return afterAnnonce.split('/').lastOrNull { seg ->
            seg.isNotBlank() && !RESERVED_SEGMENTS.contains(seg.lowercase()) && !seg.endsWith("pieces")
        }
    }

    private fun normalizeDeal(adTypeFr: String?): String {
        val raw = adTypeFr?.trim()?.lowercase().orEmpty()
        return when (raw) {
            "vente", "achat", "buy", "sale" -> "vente"
            "location", "rent" -> "location"
            else -> "location"
        }
    }

    private fun citySlug(city: String?, postalCode: String?): String {
        val c = city?.trim().orEmpty()
        if (c.contains(' ') || c.matches(Regex(".*\\d.*e", RegexOption.IGNORE_CASE))) {
            return slugify(c)
        }
        val postal = postalCode?.trim().orEmpty()
        if (postal.startsWith("750") && postal.length == 5) {
            return parisArrondissementSlug(postal)
        }
        if (postal.startsWith("690") && postal.length == 5 &&
            (c.equals("Lyon", ignoreCase = true) || c.isBlank())
        ) {
            val arr = postal.takeLast(2).trimStart('0')
            return "lyon-${arr}e"
        }
        if (c.isNotBlank()) {
            return slugify(c)
        }
        if (postal.length >= 2) {
            return slugify(postal)
        }
        return "france"
    }

    private fun parisArrondissementSlug(postal: String): String {
        val arr = postal.takeLast(2).trimStart('0').toIntOrNull() ?: return "paris"
        if (arr == 1) return "paris-1er"
        return "paris-${arr}e"
    }

    private fun propertyTypeSlug(propertyType: String?): String = when (propertyType?.lowercase()) {
        "flat", "apartment" -> "appartement"
        "house", "home" -> "maison"
        "parking", "garage" -> "parking"
        "land", "terrain" -> "terrain"
        "building" -> "immeuble"
        "shop", "commercial" -> "local"
        else -> "appartement"
    }

    private fun slugify(value: String): String =
        value.lowercase()
            .replace('à', 'a').replace('â', 'a').replace('ä', 'a')
            .replace('ç', 'c')
            .replace('è', 'e').replace('é', 'e').replace('ê', 'e').replace('ë', 'e')
            .replace('î', 'i').replace('ï', 'i')
            .replace('ô', 'o').replace('ö', 'o')
            .replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    private val RESERVED_SEGMENTS = setOf(
        "location", "vente", "achat", "appartement", "maison", "parking", "terrain",
        "immeuble", "local", "france",
    )

    private const val BASE = "https://www.bienici.com"
}
