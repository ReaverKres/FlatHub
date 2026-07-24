package listing.it.trovacasa

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import listing.it.isItSaleDeal
import utils.stripHtmlToPlainText

/**
 * TrovaCasa.it SSR list + detail → [AppFlat]. EUR → [AppFlat.mainPrice].
 * See tmp/it/api/trovacasa/NOTES.md.
 */
object TrovacasaHtmlParser {
    private const val SITE_ORIGIN = "https://www.trovacasa.it"
    private const val CDN_LIST = "https://cdn.trovacasa.it/annunci/FB"
    private const val CDN_DETAIL = "https://cdn.trovacasa.it/annunci/IMM-M-C"
    private const val PIC_ORIGIN = "https://pic.trovacasa.it/image"

    private val cardSplit = "immobileListing__card js_immobileListing_card"
    private val idRe = Regex("""<a name="(\d+)"\s+style="visibility:\s*hidden;"""")
    private val hrefRe = Regex("""href="(/annunci/[^"]+)"""")
    private val priceRe = Regex("""class="card__price">([^<]+)<""", RegexOption.IGNORE_CASE)
    private val cardInfoRe = Regex("""class="card__info">([^<]+)<""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""(\d+)\s*m(?:&#xB2;|²)?""", RegexOption.IGNORE_CASE)
    private val roomsRe = Regex("""(\d+)\s*locali""", RegexOption.IGNORE_CASE)
    private val titleRe = Regex("""class="card__title[^"]*">([^<]+)<""", RegexOption.IGNORE_CASE)
    private val listDescRe =
        Regex("""class="card__description">([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
    private val photoListRe = Regex("""data-src-list="([^"]+)"""")
    private val photoImgRe = Regex("""data-src="(https://(?:cdn|pic)\.trovacasa\.it[^"]+)"""")
    private val detailLatRe = Regex("""var latitude\s*=\s*([\d.\-]+)\s*;""")
    private val detailLngRe = Regex("""var longitude\s*=\s*([\d.\-]+)\s*;""")
    private val detailDescRe = Regex(
        """class="(?:js_fit_descr|fit_descr)[^"]*">([\s\S]*?)</div>""",
        RegexOption.IGNORE_CASE,
    )
    private val detailPriceRe = Regex("""(\d[\d.,]*)\s*&#x20AC;|(\d[\d.,]*)\s*€""")
    private val detailAreaRe = Regex("""(\d+)\s*m(?:&#xB2;|²)""", RegexOption.IGNORE_CASE)
    private val detailRoomsTableRe = Regex(
        """Numero locali:\s*</dt>\s*<dd class="description">(\d+)""",
        RegexOption.IGNORE_CASE,
    )
    private val galleryJsonRe =
        Regex("""addImagesToGallery\(\[(.*?)\]\)""", RegexOption.DOT_MATCHES_ALL)
    private val galleryPhotoRe = Regex(""""src":"(https://(?:cdn|pic)\.trovacasa\.it[^"]+)"""")
    private val detailAddressRe = Regex("""class="indirizzo[^"]*"[^>]*>[^<]*</[^>]+>([^<]+)<""")
    private val telRe = Regex("""href="tel:([^"]+)"""")
    private val metaDescRe = Regex("""<meta name="description"\s+content="([^"]+)"""")

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        if (!html.contains(cardSplit)) return emptyList()
        val indices = cardSplit.toRegex().findAll(html).map { it.range.first }.toList()
        if (indices.isEmpty()) return emptyList()
        return indices.mapIndexedNotNull { index, start ->
            val end = indices.getOrNull(index + 1) ?: minOf(start + 12_000, html.length)
            val chunk = html.substring(start, end)
            runCatching { mapCard(chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val lat = detailLatRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = detailLngRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val description = parseDetailDescription(html) ?: base.description
        val imageUrls = parseDetailPhotos(html).ifEmpty { base.imageUrls.orEmpty() }
        val phones = telRe.findAll(html)
            .map { it.groupValues[1].decodeHtml().filter { ch -> ch.isDigit() || ch == '+' } }
            .filter { it.length >= 9 }
            .distinct()
            .take(2)
            .toList()
            .ifEmpty { null }
        val price = detailPriceRe.find(html)?.let { match ->
            (match.groupValues[1].ifBlank { match.groupValues[2] })
                .replace(".", "")
                .replace(",", ".")
                .toDoubleOrNull()
        }
        val area = detailAreaRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val rooms = parseRooms(html) ?: base.rooms
        val address = detailAddressRe.find(html)?.groupValues?.get(1)?.decodeHtml()?.trim()
            ?: base.address

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            coordinates = coords,
            imageUrls = imageUrls.distinct().ifEmpty { null },
            mainPrice = price ?: base.mainPrice,
            totalArea = area ?: base.totalArea,
            rooms = rooms ?: base.rooms,
            address = address,
            contactInformation = ContactInformation(
                phones = phones ?: base.contactInformation?.phones,
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.TROVACASA,
        flatDetailUrl = detailUrl ?: SITE_ORIGIN,
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        secondPrice = null,
        mainPrice = null,
        rooms = null,
        district = null,
        address = null,
        metroStation = null,
        description = null,
        yearBuilt = null,
        totalArea = null,
        livingArea = null,
        kitchenArea = null,
        floor = null,
        totalFloors = null,
        sleepingPlaces = null,
        isStudio = null,
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
        owner = false,
    )

    private fun mapCard(chunk: String, adType: AdType): AppFlat? {
        val adId = idRe.find(chunk)?.groupValues?.get(1)?.toLongOrNull()
            ?: return null
        val path = hrefRe.find(chunk)?.groupValues?.get(1) ?: return null
        val detailUrl = "$SITE_ORIGIN$path"
        val priceText = priceRe.find(chunk)?.groupValues?.get(1)?.decodeHtml().orEmpty()
        val price = priceText
            .replace("€", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()
        val title = titleRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val area =
            parseAreaFromInfos(chunk) ?: areaRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val rooms = parseRooms(chunk)
        val description = listDescRe.find(chunk)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 20 }
        val imageUrls = parseListPhotos(chunk)
        val mappedAdType = if (adType.isItSaleDeal()) AdType.SALE else AdType.RENT

        return AppFlat(
            adId = adId,
            adType = mappedAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.TROVACASA,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = imageUrls.distinct().ifEmpty { null },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = null,
            address = title,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = null,
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
            owner = false,
        )
    }

    private fun parseDetailDescription(html: String): String? {
        detailDescRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
            ?.let { return it }

        return metaDescRe.find(html)?.groupValues?.get(1)
            ?.decodeHtml()
            ?.trim()
            ?.takeIf { it.length > 40 }
    }

    private fun parseListPhotos(html: String): List<String> {
        photoImgRe.find(html)?.groupValues?.get(1)?.let { raw ->
            return listOf(normalizeListPhotoUrl(raw.decodeHtml()))
        }
        photoListRe.find(html)?.groupValues?.get(1)?.let { raw ->
            val assetId = raw.split('|').firstOrNull()?.trim().orEmpty()
            if (assetId.isNotEmpty()) {
                return listOf(buildListPhotoUrl(assetId))
            }
        }
        return emptyList()
    }

    private fun parseDetailPhotos(html: String): List<String> {
        galleryJsonRe.find(html)?.groupValues?.get(1)?.let { jsonArray ->
            val fromGallery = galleryPhotoRe.findAll(jsonArray)
                .map { it.groupValues[1].replace("\\/", "/") }
                .distinct()
                .toList()
            if (fromGallery.isNotEmpty()) return fromGallery
        }
        photoListRe.find(html)?.groupValues?.get(1)?.let { raw ->
            return raw.split('|')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map(::buildDetailPhotoUrl)
        }
        return photoImgRe.findAll(html)
            .map { it.groupValues[1].decodeHtml() }
            .map { url ->
                when {
                    url.contains("pic.trovacasa.it/image/") -> url
                    url.contains("/annunci/mlis/") -> url.replace(
                        "/annunci/mlis/",
                        "/annunci/IMM-M-C/"
                    )

                    url.contains("/annunci/FB/") -> url.replace("/annunci/FB/", "/annunci/IMM-M-C/")
                    else -> url
                }
            }
            .distinct()
            .toList()
    }

    private fun buildListPhotoUrl(assetId: String): String {
        if (assetId.startsWith("X_")) {
            val photoId = assetId.substringAfterLast('_')
            return "$PIC_ORIGIN/$photoId/s-c.jpg"
        }
        return "$CDN_LIST/$assetId/foto.jpg"
    }

    private fun buildDetailPhotoUrl(assetId: String): String {
        val photoId = assetId.substringAfterLast('_')
        if (assetId.startsWith("X_")) {
            return "$PIC_ORIGIN/$photoId/l.jpg"
        }
        return "$CDN_DETAIL/$assetId/$photoId.jpg"
    }

    private fun parseRooms(html: String): Int? {
        detailRoomsTableRe.find(html)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return roomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseAreaFromInfos(html: String): Double? =
        cardInfoRe.findAll(html)
            .map { it.groupValues[1].decodeHtml() }
            .firstNotNullOfOrNull { info ->
                areaRe.find(info)?.groupValues?.get(1)?.toDoubleOrNull()
            }

    private fun normalizeListPhotoUrl(url: String): String {
        val clean = url.replace("&amp;", "&")
        if (clean.contains("pic.trovacasa.it/image/")) {
            return clean.replace(Regex("/[^/]+\\.jpg$"), "/s-c.jpg")
        }
        if (clean.contains("cdn.trovacasa.it/annunci/mlis/")) {
            return clean.replace("/annunci/mlis/", "/annunci/FB/")
        }
        if (clean.contains("cdn.trovacasa.it/annunci/IMM-M-C/")) {
            return clean.replace("/annunci/IMM-M-C/", "/annunci/FB/")
        }
        return clean
    }

    private fun String.decodeHtml(): String =
        replace("&#x20AC;", "€")
            .replace("&#xB2;", "²")
            .replace("&#xE0;", "à")
            .replace("&#xE8;", "è")
            .replace("&#xE9;", "é")
            .replace("&#xEC;", "ì")
            .replace("&#xF2;", "ò")
            .replace("&#xF9;", "ù")
            .replace("&#x2B;", "+")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("â€™", "'")
            .trim()
}
