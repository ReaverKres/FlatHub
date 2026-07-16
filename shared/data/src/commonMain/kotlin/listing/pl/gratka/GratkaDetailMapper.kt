package listing.pl.gratka

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import utils.stripHtmlToPlainText
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object GratkaDetailMapper {
    fun mergeInto(base: AppFlat, root: JsonObject): AppFlat {
        val node = root["data"].asObjectOrNull()
            ?.get("getProperty").asObjectOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )

        val location = node["location"].asObjectOrNull()
        val mapCenter = location?.get("map").asObjectOrNull()
            ?.get("center").asObjectOrNull()
        val lat = mapCenter?.get("latitude").doubleOrNull()
        val lon = mapCenter?.get("longitude").doubleOrNull()
        val coordinates = if (lat != null && lon != null) {
            Coordinates(lat, lon)
        } else {
            base.coordinates
        }

        val street = location?.get("street").contentOrNull()
        val locationParts = location?.get("location").asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            .orEmpty()
        val city = locationParts.getOrNull(0)
        val district = locationParts.getOrNull(1) ?: base.district
        val address = listOfNotNull(street, district, city).joinToString(", ")
            .ifBlank { base.address }

        val contact = node["contact"].asObjectOrNull()
        val person = contact?.get("person").asObjectOrNull()
        val company = contact?.get("company").asObjectOrNull()
        val ownerName = person?.get("name").contentOrNull()
            ?: company?.get("name").contentOrNull()
            ?: base.contactInformation?.ownerName
        val phones = (person?.get("phones") ?: company?.get("phones"))
            .asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: base.contactInformation?.phones

        val photos = node["photos"].asArrayOrNull()?.mapNotNull { photo ->
            decodePhotoUrl(photo.asObjectOrNull()?.get("id").contentOrNull())
        }?.takeIf { it.isNotEmpty() } ?: base.imageUrls

        val description = (
                node["description"].contentOrNull() ?: base.description
                ).stripHtmlToPlainText()

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = coordinates,
            imageUrls = photos,
            description = description,
            district = district,
            address = address,
            totalArea = node["area"].doubleOrNull() ?: base.totalArea,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodePhotoUrl(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            Base64.decode(encoded).decodeToString()
                .replace("http://", "https://")
        }.getOrNull()
    }
}
