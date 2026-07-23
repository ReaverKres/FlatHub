package listing.fr.bienici

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.fr.stableFrAdId
import utils.stripHtmlToPlainText
import kotlin.time.Instant

object BieniciFlatMapper {
    private val rentFromDescRe = Regex(
        """Loyer[^0-9]{0,80}([\d\s\u00a0\u202f.,]+)\s*(?:€|euros?)""",
        RegexOption.IGNORE_CASE,
    )
    private val rentCcDescRe = Regex(
        """([\d\s\u00a0\u202f.,]+)\s*euros?\s*CC""",
        RegexOption.IGNORE_CASE,
    )

    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val ads = root["realEstateAds"].asArrayOrNull()
            ?: return emptyList()
        return ads.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapAd(item, adType, isDetail = false) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, detail: JsonObject): AppFlat {
        val mapped = runCatching { mapAd(detail, base.adType ?: AdType.RENT, isDetail = true) }
            .getOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = mapped.description?.takeIf { it.length > (base.description?.length ?: 0) }
                ?: base.description,
            mainPrice = mapped.mainPrice ?: base.mainPrice,
            mainPriceSquare = mapped.mainPriceSquare ?: base.mainPriceSquare,
            contactInformation = ContactInformation(
                phones = mapped.contactInformation?.phones ?: base.contactInformation?.phones,
                ownerName = mapped.contactInformation?.ownerName
                    ?: base.contactInformation?.ownerName,
            ),
            coordinates = mapped.coordinates ?: base.coordinates,
            imageUrls = mapped.imageUrls?.ifEmpty { null } ?: base.imageUrls,
            rooms = mapped.rooms ?: base.rooms,
            totalArea = mapped.totalArea ?: base.totalArea,
            district = mapped.district ?: base.district,
            address = mapped.address ?: base.address,
            flatDetailUrl = mapped.flatDetailUrl,
            publishedAt = mapped.publishedAt ?: base.publishedAt,
            publishedAtServer = mapped.publishedAtServer ?: base.publishedAtServer,
            publishedAtUi = mapped.publishedAtUi ?: base.publishedAtUi,
        )
    }

    private fun mapAd(item: JsonObject, adType: AdType, isDetail: Boolean): AppFlat {
        val externalId = item["id"].contentOrNull() ?: error("missing id")
        val isSale = adType is AdType.SALE ||
                item["transactionType"].contentOrNull() == "buy" ||
                item["adType"].contentOrNull() == "buy"
        val resolvedType = when {
            isSale && adType is AdType.COMMERCIAL -> adType
            isSale -> AdType.SALE
            else -> adType
        }

        val descriptionHtml = item["description"].contentOrNull()
        val description = descriptionHtml?.stripHtmlToPlainText()
        val title = item["generatedTitle"].contentOrNull()
            ?: item["title"].contentOrNull()

        val rawPrice = item["price"].doubleOrNull()
        val rentWithout = item["rentWithoutCharges"].doubleOrNull()
        val charges = item["charges"].doubleOrNull()
        val area = item["surfaceArea"].doubleOrNull()
        val mainPrice = resolvePrice(
            isSale = isSale,
            rawPrice = rawPrice,
            rentWithout = rentWithout,
            charges = charges,
            description = descriptionHtml,
            area = area,
        )
        val rooms = item["roomsQuantity"].intOrNull()
        val city = item["city"].contentOrNull()
        val postal = item["postalCode"].contentOrNull()
        val district = item["district"].asObjectOrNull()
            ?.get("libelle").contentOrNull()
            ?: item["district"].asObjectOrNull()?.get("name").contentOrNull()
        val address = listOfNotNull(district, postal, city).joinToString(", ").ifBlank { city }

        val pos = item["blurInfo"].asObjectOrNull()?.get("position").asObjectOrNull()
        val lat = pos?.get("lat").doubleOrNull()
        val lng = pos?.get("lng").doubleOrNull() ?: pos?.get("lon").doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null

        val photos = item["photos"].asArrayOrNull()?.mapNotNull { p ->
            p.asObjectOrNull()?.get("url").contentOrNull()
        }

        val phone = item["contactRelativeData"].asObjectOrNull()
            ?.get("phoneToDisplay").contentOrNull()
            ?.takeIf { it.isNotBlank() }
        val owner = item["contactRelativeData"].asObjectOrNull()
            ?.get("agencyNameToDisplay").contentOrNull()

        val priceSquare = if (mainPrice != null && area != null && area > 0) {
            mainPrice / area
        } else null

        val (publishedAt, publishedAtServer) = resolvePublishedAt(item)
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }
        val adTypeFr = item["adTypeFR"].contentOrNull()
            ?: if (isSale) "vente" else "location"
        val propertyTypeRaw = item["propertyType"].contentOrNull()
        val detailUrl = BieniciCities.detailUrl(
            externalId = externalId,
            adTypeFr = adTypeFr,
            city = item["city"].contentOrNull(),
            postalCode = postal,
            propertyType = propertyTypeRaw,
            rooms = rooms,
        )

        return AppFlat(
            adId = stableFrAdId(externalId),
            adType = resolvedType,
            flatDevInfo = FlatDevInfo(
                isDetailData = isDetail,
                isDetailLoaded = isDetail,
            ),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = owner,
            ),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.BIENICI,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = publishedAtServer,
            publishedAtUi = publishedAtUi,
            imageUrls = photos,
            secondPrice = null,
            mainPrice = mainPrice,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description ?: title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rooms == 1,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = null,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = null,
            mainPriceSquare = priceSquare,
            secondPriceSquare = null,
        )
    }

    /**
     * Bien'ici list JSON often stores nonsense in [price] (e.g. 14 € for Paris 50 m²).
     * Real totals: description "Loyer … €", rentWithout+charges (€/month), or per-m² × area.
     * Never use agencyRentalFee — that is the agency fee, not monthly rent.
     */
    private fun resolvePrice(
        isSale: Boolean,
        rawPrice: Double?,
        rentWithout: Double?,
        charges: Double?,
        description: String?,
        area: Double?,
    ): Double? {
        if (isSale) {
            return rawPrice?.takeIf { it >= 10_000.0 }
        }

        parseRentFromDescription(description, minTotal = 150.0)?.let { return it }

        val chargesSafe = charges?.coerceAtLeast(0.0) ?: 0.0
        if (rentWithout != null && rentWithout >= 150.0) {
            return rentWithout + chargesSafe
        }
        if (rawPrice != null && rawPrice >= 150.0) {
            return rawPrice
        }

        if (area != null && area >= 12.0) {
            if (rentWithout != null && rentWithout in 5.0..150.0) {
                val total = (rentWithout + chargesSafe) * area
                if (total >= 250.0) return total
            }
            if (rawPrice != null && rawPrice in 5.0..150.0) {
                val total = rawPrice * area
                if (total >= 250.0) return total
            }
        }

        if (rawPrice != null && rawPrice in 50.0..150.0) {
            return rawPrice
        }

        return parseRentFromDescription(description, minTotal = 50.0)
    }

    private fun parseRentFromDescription(description: String?, minTotal: Double): Double? {
        if (description.isNullOrBlank()) return null
        val plain = description.stripHtmlToPlainText() ?: return null
        val candidates = listOfNotNull(
            rentFromDescRe.find(plain)?.groupValues?.getOrNull(1),
            rentCcDescRe.find(plain)?.groupValues?.getOrNull(1),
        )
        for (raw in candidates) {
            val amount = parseEuroAmount(raw) ?: continue
            if (amount >= minTotal) return amount
        }
        return null
    }

    private fun parseEuroAmount(raw: String): Double? =
        raw.replace('\u00a0', ' ')
            .replace('\u202f', ' ')
            .replace(" ", "")
            .replace(',', '.')
            .toDoubleOrNull()

    /**
     * Bien'ici often blanks real dates (`1970-01-01`) when [canSeeRealDates] is false.
     * Prefer real publication/modification, then public [thresholdDate] (never invent "now").
     */
    private fun resolvePublishedAt(item: JsonObject): Pair<Instant?, String?> {
        val candidates = listOf(
            item["publicationDate"].contentOrNull(),
            item["modificationDate"].contentOrNull(),
            item["thresholdDate"].contentOrNull(),
        )
        for (raw in candidates) {
            val instant = parseUsableInstant(raw) ?: continue
            return instant to raw
        }
        return null to null
    }

    private fun parseUsableInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return null
        // Epoch placeholder when Bien'ici hides real dates from anonymous clients.
        if (instant.epochSeconds <= 0L) return null
        return instant
    }
}
