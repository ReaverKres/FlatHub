import entities.BalconyType
import entities.BathroomType
import entities.BuildingImprovement
import entities.PrepaymentType
import entities.RepairType
import entities.WindowDirection
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class AppFlat @OptIn(ExperimentalTime::class) constructor(
    val flatPlatform: FlatPlatform,
    val flatDetailUrl: String,
    val adId: Long,
    val publishedAt: Instant?,
    val timeAgo: String?,
    val imageUrls: List<String>?,
    val priceUsd: Double,
    val priceByn: Double,
    val rooms: Int,
    val district: String?,
    val address: String?,
    val coordinates: Pair<Double, Double>?,
    val metroStation: String?,
    val description: String?,
    val yearBuilt: Int?,
    val additionalParams: AdditionalParams?,
    // Новые поля
    val totalArea: Double?,
    val floor: Int?,
    val totalFloors: Int?,
    val sleepingPlaces: Int?,
    val isStudio: Boolean,
    val bathroomType: BathroomType?,
    val balconyType: BalconyType?,
    val repairType: RepairType?,
    val windowDirections: List<WindowDirection>?,
    val buildingImprovements: List<BuildingImprovement>?,
    val prepaymentType: PrepaymentType?
)

data class AdditionalParams(
    val forWhom: List<String>?,
    val hasWashingMachine: Boolean,
    val hasStove: Boolean,
    val hasMicrowave: Boolean,
    val hasWifi: Boolean,
    val hasFurniture: Boolean,
    val hasConditioner: Boolean
)