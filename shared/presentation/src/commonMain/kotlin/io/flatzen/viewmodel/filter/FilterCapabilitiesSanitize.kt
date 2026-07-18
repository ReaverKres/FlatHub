package io.flatzen.viewmodel.filter

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.isCommercial
import listing.core.SourceCapabilities

/**
 * Aligns UI filter state with what at least one platform for [country] can search.
 */
internal fun FilterState.sanitizeForCountryCapabilities(
    country: CountryCode,
    caps: SourceCapabilities,
): FilterState {
    var nextAdType = adType
    when {
        nextAdType is AdType.DAILY && !caps.supportsDaily -> nextAdType = AdType.RENT
        nextAdType.isCommercial && !caps.supportsCommercial -> nextAdType = AdType.RENT
        nextAdType is AdType.SALE && !caps.supportsSale -> nextAdType = AdType.RENT
        nextAdType is AdType.RENT && !caps.supportsRent && caps.supportsSale ->
            nextAdType = AdType.SALE
    }

    val nextRoomOnly = roomOnly && caps.supportsRoom && nextAdType is AdType.RENT

    val nextCommercial = when {
        !caps.supportsCommercial || !caps.supportsCommercialPropertyTypes ->
            commercial.copy(commercialPropertyType = null)

        commercial.commercialPropertyType.isNullOrEmpty() ->
            commercial.copy(commercialPropertyType = defaultCommercialPropertyTypeInfos(country))

        else -> commercial
    }

    val nextBooking = if (nextAdType is AdType.DAILY) bookingDatesFilter else null

    return copy(
        adType = nextAdType,
        roomOnly = nextRoomOnly,
        commercial = nextCommercial,
        bookingDatesFilter = nextBooking,
    )
}

private fun defaultCommercialPropertyTypeInfos(
    country: CountryCode,
): List<CommercialPropertyTypeInfo> {
    val defaultType = CommercialPropertyType.defaultFor(country)
    return CommercialPropertyType.instancesFor(country).map {
        CommercialPropertyTypeInfo(
            selected = it == defaultType,
            commercialPropertyType = it,
            commercialPropertyTypeName = CommercialPropertyTypeInfo.commercialPropertyTypeName(it),
        )
    }
}
