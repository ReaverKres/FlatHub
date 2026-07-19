package listing.ge.binebi

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import utils.stripHtmlToPlainText

/**
 * Regex HTML parser for Binebi list cards (`div.list-item`).
 */
object BinebiHtmlParser {
    private val hrefRe =
        Regex("""href="(gancxadebebi/\d+-[^"]+)"""", RegexOption.IGNORE_CASE)
    private val titleRe =
        Regex("""<a[^>]*title="([^"]+)"[^>]*href="gancxadebebi/""", RegexOption.IGNORE_CASE)
    private val priceRe =
        Regex("""data-price="(\d+(?:\.\d+)?)"[^>]*class="price"""", RegexOption.IGNORE_CASE)
    private val valuteRe =
        Regex("""class="valute"\s*>\s*([^<]+)""", RegexOption.IGNORE_CASE)
    private val addressRe =
        Regex(
            """class="fa fa-map-marker"></i>\s*([^<]+)""",
            RegexOption.IGNORE_CASE,
        )
    private val areaRe = Regex("""(\d+(?:[.,]\d+)?)\s*მ²""")
    private val roomsRe = Regex("""(\d+)\s*ოთახ""")
    private val floorRe = Regex("""(\d+)\s*/\s*(\d+)\s*</div>\s*<div[^>]*>\s*სართული""")
    private val descRe =
        Regex("""class="item-description">\s*([\s\S]*?)\s*</div>""", RegexOption.IGNORE_CASE)
    private val imgRe =
        Regex("""data-src="(https://cdn\.binebi\.ge/[^"]+)"""", RegexOption.IGNORE_CASE)

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        // Split by list-item markers — more reliable than one giant regex.
        val parts = html.split(Regex("""<div\s+class="list-item"\s+data-id="""))
        if (parts.size <= 1) return emptyList()
        return parts.drop(1).mapNotNull { chunk ->
            val id = Regex("""^"?(\d+)""").find(chunk)?.groupValues?.get(1)?.toLongOrNull()
                ?: return@mapNotNull null
            runCatching { mapCard(id, chunk, adType) }.getOrNull()
        }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val desc = Regex(
            """<(?:div|p)[^>]*(?:description|desc|content)[^>]*>\s*([\s\S]{20,4000}?)</(?:div|p)>""",
            RegexOption.IGNORE_CASE,
        ).findAll(html)
            .map { it.groupValues[1].stripHtmlToPlainText() }
            .firstOrNull { !it.isNullOrBlank() && it.length > (base.description?.length ?: 0) }
        val phones = Regex("""(?:\+?995)?[\s-]?(?:5\d{2}|32)[\s-]?\d{2}[\s-]?\d{2}[\s-]?\d{2}""")
            .findAll(html)
            .map { it.value.replace(Regex("""[\s-]"""), "") }
            .distinct()
            .take(3)
            .toList()
            .ifEmpty { null }
        val lat = Regex("""var\s+map_lat\s*=\s*'([-\d.]+)'""").find(html)?.groupValues?.get(1)
            ?.toDoubleOrNull()
            ?: Regex(""""map_lat"\s*:\s*"?([-\d.]+)"""").find(html)?.groupValues?.get(1)
                ?.toDoubleOrNull()
        val lon = Regex("""var\s+map_lon\s*=\s*'([-\d.]+)'""").find(html)?.groupValues?.get(1)
            ?.toDoubleOrNull()
            ?: Regex(""""map_lon"\s*:\s*"?([-\d.]+)"""").find(html)?.groupValues?.get(1)
                ?.toDoubleOrNull()
        val coords = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            Coordinates(lat, lon)
        } else {
            base.coordinates
        }
        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = desc ?: base.description,
            coordinates = coords,
            contactInformation = ContactInformation(
                phones = phones ?: base.contactInformation?.phones,
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    private fun mapCard(id: Long, chunk: String, adType: AdType): AppFlat {
        val href = hrefRe.find(chunk)?.groupValues?.get(1)
        val detailUrl = href?.let { "https://binebi.ge/$it" }
            ?: "https://binebi.ge/gancxadebebi/$id"
        val title = titleRe.find(chunk)?.groupValues?.get(1)
            ?: href?.substringAfter("$id-")?.replace('-', ' ')
        val priceRaw = priceRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val valute = valuteRe.find(chunk)?.groupValues?.get(1)?.trim().orEmpty()
        val isGel = valute.contains('₾') || valute.contains("GEL", ignoreCase = true) ||
                (!valute.contains('$') && priceRaw != null)
        val address = addressRe.find(chunk)?.groupValues?.get(1)?.trim()
        val district = address?.split(',')?.getOrNull(1)?.trim()
        val area = areaRe.find(chunk)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val rooms = roomsRe.find(chunk)?.groupValues?.get(1)?.toIntOrNull()
        val floorMatch = floorRe.find(chunk)
        val floor = floorMatch?.groupValues?.get(1)?.toIntOrNull()
        val totalFloors = floorMatch?.groupValues?.get(2)?.toIntOrNull()
        val description = descRe.find(chunk)?.groupValues?.get(1).stripHtmlToPlainText()
            ?: title
        val images = imgRe.findAll(chunk).map { it.groupValues[1] }.distinct().toList()
            .ifEmpty { null }

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.BINEBI,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            mainPrice = priceRaw,
            secondPrice = null,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = null,
            isStudio = rooms == 0,
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
        )
    }
}
