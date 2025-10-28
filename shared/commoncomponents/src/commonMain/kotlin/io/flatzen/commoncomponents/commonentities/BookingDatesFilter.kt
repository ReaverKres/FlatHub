package io.flatzen.commoncomponents.commonentities

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class BookingDatesFilter(
    val dateFrom: Instant,
    val dateTo: Instant,
    val timeZone: TimeZone? = null
)