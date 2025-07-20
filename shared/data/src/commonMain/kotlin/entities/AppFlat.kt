package entities

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class AppFlat @OptIn(ExperimentalTime::class) constructor(
    val publishedAt: Instant?,
    val timeAgo: String?,
    val priceUsd: Int,
    val priceByn: Int,
    val rooms: Int,
    val district: String,
    val address: String,
    val coordinates: Pair<Double, Double>?,
    val metroStation: String?,
    val description: String?,
    val yearBuilt: Int?,
    val additionalParams: AdditionalParams
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