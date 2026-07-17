package listing.th.livinginsider

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import listing.th.ThThaiDate
import utils.stripHtmlToPlainText

/**
 * Livinginsider searchword HTML → [AppFlat]. THB → [AppFlat.priceByn].
 * See tmp/th/api/livinginsider/NOTES.md.
 */
object LivinginsiderMapper {
    private val detailHrefRe =
        Regex("""href="(https://www\.livinginsider\.com/detail/([^"]+)-(\d+))"""")
    private val textPriceRe =
        Regex(
            """class="text_price"[^>]*>\s*(?:<span[^>]*>[^<]*</span>)?\s*([\d,]+(?:\.\d+)?)""",
            RegexOption.IGNORE_CASE,
        )
    private val imgRe =
        Regex("""(?:src|data-src)="(https://www\.livinginsider\.com/upload/[^"]+)"""")
    private val bedsRe = Regex("""(\d+)\s*(?:ห้องนอน|Bedroom|bedroom)""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""([\d.]+)\s*ตร\.?\s*ม""")
    private val titleRe = Regex("""title="([^"]{8,240})"""")
    private val lastDateRe =
        Regex(
            """istock-lastdate[^>]*>\s*([\s\S]*?)</""",
            RegexOption.IGNORE_CASE,
        )
    private val ownerTitleRe =
        Regex(
            """class="owner-name"[^>]*title="([^"]+)"""",
            RegexOption.IGNORE_CASE,
        )
    private val nameOwnerLabelRe =
        Regex(
            """id="nameOwner"[^>]*>\s*<label[^>]*>\s*([^<]+)<""",
            RegexOption.IGNORE_CASE,
        )
    private val createdAtRe = Regex("""สร้างเมื่อ\s*([0-9/]+)""")
    private val bumpRelativeRe =
        Regex("""ดันประกาศล่าสุดเมื่อ\s*([^<]+)""")

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val prices = textPriceRe.findAll(html).map { it }.toList()
        val details = detailHrefRe.findAll(html).map { it }.toList()
        if (details.isEmpty()) return emptyList()

        val seen = LinkedHashSet<Long>()
        val out = ArrayList<AppFlat>()
        for (m in details) {
            val url = m.groupValues[1]
            val id = m.groupValues[3].toLongOrNull() ?: continue
            if (!seen.add(id)) continue
            val dealFromUrl = when {
                url.contains("for-sale") -> AdType.SALE
                url.contains("for-rent") -> AdType.RENT
                else -> adType
            }
            val idx = m.range.first
            // Prefer card root (`id="list{id}"`) — date sits ~20k chars after first detail href.
            val cardStart = html.indexOf("""id="list$id"""")
                .takeIf { it >= 0 }
                ?: html.indexOf("""data-web-id="$id"""")
                    .takeIf { it >= 0 }
                ?: (idx - 1500).coerceAtLeast(0)
            val window = html.substring(
                cardStart,
                (cardStart + 30_000).coerceAtMost(html.length),
            )
            val price = textPriceRe.find(window)?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
                ?: prices.getOrNull(out.size)?.groupValues?.get(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
            val img = imgRe.find(window)?.groupValues?.get(1)
            val beds = bedsRe.find(window)?.groupValues?.get(1)?.toIntOrNull()
            val area = areaRe.find(window)?.groupValues?.get(1)?.toDoubleOrNull()
            val title = titleRe.findAll(window)
                .map { it.groupValues[1] }
                .firstOrNull { !it.equals("Add to Favorite", ignoreCase = true) }
                ?.stripHtmlToPlainText()
            val dateRaw = lastDateRe.find(window)?.groupValues?.get(1)
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()
            val publishedAt = ThThaiDate.parse(dateRaw)
            val publishedAtUi = publishedAt?.let {
                DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
            } ?: dateRaw
            val ownerHint = resolveOwnerFlag(window)

            out += AppFlat(
                adId = id,
                adType = dealFromUrl,
                flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
                contactInformation = ContactInformation(phones = null, ownerName = null),
                coordinates = null,
                commercialInfo = null,
                flatPlatform = FlatPlatform.LIVINGINSIDER,
                flatDetailUrl = url,
                publishedAt = publishedAt,
                publishedAtServer = dateRaw,
                publishedAtUi = publishedAtUi,
                imageUrls = img?.let { listOf(it) },
                priceUsd = null,
                priceByn = price,
                rooms = beds,
                district = null,
                address = title,
                metroStation = null,
                description = title,
                yearBuilt = null,
                totalArea = area,
                livingArea = null,
                kitchenArea = null,
                floor = null,
                totalFloors = null,
                sleepingPlaces = null,
                isStudio = beds == 0,
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
                owner = ownerHint,
            )
        }
        return out
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val plain = html
            .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
        val price = Regex("""([\d,]+)\s*บาท""")
            .findAll(plain)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .firstOrNull { it >= 1000 }
        val area = Regex("""([\d.]+)\s*ตร\.?\s*ม""")
            .find(plain)?.groupValues?.get(1)?.toDoubleOrNull()
        val beds = Regex("""(\d+)\s*ห้องนอน""")
            .find(plain)?.groupValues?.get(1)?.toIntOrNull()
        val phones = Regex("""tel:([+\d]+)""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { it.groupValues[1] }
            .filter { it.length >= 9 }
            .distinct()
            .toList()
            .ifEmpty { null }
        val ownerName = ownerTitleRe.find(html)?.groupValues?.get(1)?.trim()
            ?: nameOwnerLabelRe.find(html)?.groupValues?.get(1)?.trim()
            ?: base.contactInformation?.ownerName
        val owner = resolveOwnerFlag(html, ownerName) ?: base.owner
        val dateRaw = createdAtRe.find(html)?.groupValues?.get(1)
            ?: bumpRelativeRe.find(html)?.groupValues?.get(1)?.trim()
            ?: base.publishedAtServer
        val publishedAt = ThThaiDate.parse(dateRaw) ?: base.publishedAt
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: dateRaw ?: base.publishedAtUi

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            priceByn = price ?: base.priceByn,
            totalArea = area ?: base.totalArea,
            rooms = beds ?: base.rooms,
            contactInformation = ContactInformation(
                phones = phones ?: base.contactInformation?.phones,
                ownerName = ownerName,
            ),
            publishedAt = publishedAt,
            publishedAtServer = dateRaw,
            publishedAtUi = publishedAtUi,
            owner = owner,
            description = base.description,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.LIVINGINSIDER,
        flatDetailUrl = detailUrl ?: "https://www.livinginsider.com/",
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        priceUsd = null,
        priceByn = null,
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
        owner = null,
    )

    /**
     * Owner vs agent: `เจ้าของขายเอง` → owner; display name `Person • Company` → agent.
     * Avoid matching site chrome (`นายหน้า` / Agent Finder) which appears on every page.
     */
    private fun resolveOwnerFlag(html: String, ownerName: String? = null): Boolean? {
        if (html.contains("เจ้าของขายเอง")) return true
        val name = ownerName?.trim().orEmpty()
        if (name.contains('•') || name.contains('·')) return false
        return null
    }
}
