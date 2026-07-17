package listing.pl.otodom

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import listing.core.asObjectOrNull
import listing.core.doubleOrNull
import utils.stripHtmlToPlainText

object OtodomDetailMapper {
    /**
     * Merges detail payload into an existing list [base] flat.
     */
    fun mergeInto(base: AppFlat, root: JsonObject): AppFlat {
        // `/_next/data/.../oferta/*.json` → pageProps; HTML `__NEXT_DATA__` → props.pageProps
        val pageProps = root["pageProps"]?.jsonObject
            ?: root["props"]?.jsonObject?.get("pageProps")?.jsonObject
            ?: return base
        val ad = pageProps["ad"]?.jsonObject ?: return base
        val contact = ad["contactDetails"]?.jsonObject
            ?: pageProps["contactDetails"]?.jsonObject
        val target = ad["target"]?.jsonObject
        val characteristics = charMap(ad["characteristics"]?.jsonArray)

        val coordinates = extractCoordinates(ad) ?: base.coordinates

        val phones = contact?.get("phones")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.takeIf { it.isNotEmpty() }
        val ownerName = contact?.get("name")?.jsonPrimitive?.contentOrNull
            ?: base.contactInformation?.ownerName
        val contactInfo = ContactInformation(
            phones = phones ?: base.contactInformation?.phones,
            ownerName = ownerName,
        )

        val images = ad["images"]?.jsonArray?.mapNotNull { img ->
            img.jsonObject["large"]?.jsonPrimitive?.contentOrNull
                ?: img.jsonObject["medium"]?.jsonPrimitive?.contentOrNull
        }?.takeIf { it.isNotEmpty() } ?: base.imageUrls

        val description = (
                ad["description"]?.jsonPrimitive?.contentOrNull ?: base.description
                ).stripHtmlToPlainText()

        val street = ad["location"]?.jsonObject
            ?.get("address")?.jsonObject
            ?.get("street")?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull
        val district = districtFromLocation(ad["location"]?.jsonObject) ?: base.district
        val city = cityFromLocation(ad["location"]?.jsonObject)
        val address = listOfNotNull(street, district, city)
            .joinToString(", ")
            .ifBlank { base.address }

        val advertiserType = ad["advertiserType"]?.jsonPrimitive?.contentOrNull
            ?: contact?.get("type")?.jsonPrimitive?.contentOrNull
        val owner = when (advertiserType?.lowercase()) {
            "private", "owner" -> true
            "agency", "business" -> false
            else -> base.owner
        }

        val rooms = characteristics["rooms_num"]?.toIntOrNull()
            ?: targetStringList(target, "Rooms_num").firstOrNull()?.toIntOrNull()
            ?: base.rooms
        val area = characteristics["m"]?.replace(',', '.')?.toDoubleOrNull()
            ?: target?.get("Area")?.jsonPrimitive?.doubleOrNull
            ?: target?.get("Area")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: base.totalArea
        val floor = parseFloorToken(
            characteristics["floor_no"]
                ?: targetStringList(target, "Floor_no").firstOrNull(),
        ) ?: base.floor
        val totalFloors = characteristics["building_floors_num"]?.toIntOrNull()
            ?: target?.get("Building_floors_num")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: base.totalFloors
        val yearBuilt = characteristics["build_year"]?.toIntOrNull()
            ?: target?.get("Build_year")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: base.yearBuilt

        val extras = targetStringList(target, "Extras_types")
        val equipment = targetStringList(target, "Equipment_types")
        val security = targetStringList(target, "Security_types")
        val media = targetStringList(target, "Media_types")

        val balcony = when {
            extras.any { it.equals("balcony", true) } -> "balkon"
            extras.any { it.equals("terrace", true) } -> "taras"
            extras.any { it.equals("loggia", true) } -> "loggia"
            else -> base.balcony
        }
        val parkingInfo = when {
            extras.any { it.contains("garage", ignoreCase = true) } -> "garaż"
            extras.any { it.contains("parking", ignoreCase = true) } -> "parking"
            else -> base.parkingInfo
        }
        val condition = humanize(
            characteristics["construction_status"]
                ?: targetStringList(target, "Construction_status").firstOrNull(),
        ) ?: base.condition
        val repairType = humanize(
            characteristics["building_type"]
                ?: targetStringList(target, "Building_type").firstOrNull(),
        ) ?: base.repairType
        val windowDirections = targetStringList(target, "Windows_type")
            .map { humanize(it) ?: it }
            .ifEmpty { base.windowDirections }
        val amenities = (extras + security + media)
            .mapNotNull { humanize(it) }
            .distinct()
            .ifEmpty { base.amenities }
        val kitchenEquipment = equipment
            .mapNotNull { humanize(it) }
            .distinct()
            .ifEmpty { base.kitchenEquipment }
        val buildingImprovements = buildList {
            humanize(
                characteristics["heating"] ?: targetStringList(target, "Heating").firstOrNull(),
            )?.let { add("ogrzewanie: $it") }
            if (extras.any { it.equals("lift", true) }) add("winda")
        }.ifEmpty { base.buildingImprovements }
        val deposit = characteristics["deposit"]
        val rent = characteristics["rent"]
        val prepaymentType = listOfNotNull(
            deposit?.let { "kaucja: $it PLN" },
            rent?.let { "czynsz: $it PLN" },
        ).joinToString(" · ").ifBlank { base.prepaymentType }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            contactInformation = contactInfo,
            coordinates = coordinates,
            imageUrls = images,
            description = description,
            address = address,
            district = district,
            owner = owner,
            rooms = rooms,
            totalArea = area,
            floor = floor,
            totalFloors = totalFloors,
            yearBuilt = yearBuilt,
            balcony = balcony,
            parkingInfo = parkingInfo,
            condition = condition,
            repairType = repairType,
            windowDirections = windowDirections,
            amenities = amenities,
            kitchenEquipment = kitchenEquipment,
            buildingImprovements = buildingImprovements,
            prepaymentType = prepaymentType,
            flatDetailUrl = ad["url"]?.jsonPrimitive?.contentOrNull ?: base.flatDetailUrl,
        )
    }

    fun slugFromDetailUrl(url: String): String? {
        val marker = "/oferta/"
        val idx = url.indexOf(marker)
        if (idx < 0) return null
        val after = url.substring(idx + marker.length)
        return after.substringBefore('?').substringBefore('/').ifBlank { null }
    }

    /**
     * Otodom puts GPS in several shapes across list/detail / `__NEXT_DATA__`:
     * - `ad.coordinates.{latitude,longitude}` (common on detail)
     * - `ad.location.coordinates.{latitude,longitude}`
     * - `ad.location.{latitude,longitude}`
     * - `ad.map.{lat,lon}` / `location.map…`
     *
     * Must use [asObjectOrNull] — list payloads often have `"coordinates": null`
     * (JsonNull); kotlinx `.jsonObject` throws and wipes the whole Otodom page.
     */
    fun extractCoordinates(adOrItem: JsonObject): Coordinates? {
        fun fromPair(latEl: JsonElement?, lonEl: JsonElement?): Coordinates? {
            val lat = latEl.doubleOrNull() ?: return null
            val lon = lonEl.doubleOrNull() ?: return null
            return Coordinates(lat, lon)
        }

        fun fromObj(obj: JsonObject?, latKey: String, lonKey: String): Coordinates? =
            fromPair(obj?.get(latKey), obj?.get(lonKey))

        fromObj(
            adOrItem["coordinates"].asObjectOrNull(),
            "latitude",
            "longitude"
        )?.let { return it }
        fromObj(adOrItem["coordinates"].asObjectOrNull(), "lat", "lon")?.let { return it }

        val location = adOrItem["location"].asObjectOrNull()
        fromObj(
            location?.get("coordinates").asObjectOrNull(),
            "latitude",
            "longitude"
        )?.let { return it }
        fromObj(location, "latitude", "longitude")?.let { return it }
        fromObj(location?.get("map").asObjectOrNull(), "lat", "lon")?.let { return it }
        fromObj(location?.get("map").asObjectOrNull(), "latitude", "longitude")?.let { return it }

        fromObj(adOrItem["map"].asObjectOrNull(), "lat", "lon")?.let { return it }
        fromObj(adOrItem["map"].asObjectOrNull(), "latitude", "longitude")?.let { return it }

        return null
    }

    private fun charMap(array: JsonArray?): Map<String, String> {
        if (array == null) return emptyMap()
        return array.mapNotNull { el ->
            val obj = el.jsonObject
            val key = obj["key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val value = obj["value"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun targetStringList(target: JsonObject?, key: String): List<String> {
        val el = target?.get(key) ?: return emptyList()
        (el as? JsonArray)?.let { arr ->
            return arr.mapNotNull { it.jsonPrimitive.contentOrNull }
        }
        return listOfNotNull(el.jsonPrimitive.contentOrNull)
    }

    private fun districtFromLocation(location: JsonObject?): String? {
        val locations = location?.get("reverseGeocoding")?.jsonObject
            ?.get("locations")?.jsonArray
            ?: return null
        return locations.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .firstOrNull { it["locationLevel"]?.jsonPrimitive?.contentOrNull == "district" }
            ?.get("name")?.jsonPrimitive?.contentOrNull
    }

    private fun cityFromLocation(location: JsonObject?): String? {
        val fromAddress = location?.get("address")?.jsonObject
            ?.get("city")?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull
        if (!fromAddress.isNullOrBlank()) return fromAddress
        val locations = location?.get("reverseGeocoding")?.jsonObject
            ?.get("locations")?.jsonArray
            ?: return null
        return locations.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .firstOrNull { it["locationLevel"]?.jsonPrimitive?.contentOrNull == "city_or_village" }
            ?.get("name")?.jsonPrimitive?.contentOrNull
    }

    private fun parseFloorToken(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        Regex("(\\d+)").find(raw)?.value?.toIntOrNull()?.let { return it }
        return when (raw.uppercase()) {
            "GROUND", "GROUND_FLOOR", "PARTER", "FLOOR_0" -> 0
            "FIRST", "FLOOR_1" -> 1
            "SECOND", "FLOOR_2" -> 2
            "THIRD", "FLOOR_3" -> 3
            "FOURTH", "FLOOR_4" -> 4
            "FIFTH", "FLOOR_5" -> 5
            "SIXTH", "FLOOR_6" -> 6
            "SEVENTH", "FLOOR_7" -> 7
            "EIGHTH", "FLOOR_8" -> 8
            "NINTH", "FLOOR_9" -> 9
            "TENTH", "FLOOR_10" -> 10
            "CELLAR", "BASEMENT" -> -1
            else -> null
        }
    }

    private fun humanize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return when (raw.lowercase()) {
            "balcony" -> "balkon"
            "terrace" -> "taras"
            "loggia" -> "loggia"
            "garage" -> "garaż"
            "parking" -> "parking"
            "lift" -> "winda"
            "furniture" -> "meble"
            "washing_machine" -> "pralka"
            "dishwasher" -> "zmywarka"
            "fridge" -> "lodówka"
            "stove" -> "kuchenka"
            "oven" -> "piekarnik"
            "tv" -> "TV"
            "internet" -> "internet"
            "cable-television", "cable_television" -> "telewizja kablowa"
            "anti_burglary_door" -> "drzwi antywłamaniowe"
            "entryphone" -> "domofon"
            "monitoring" -> "monitoring"
            "ready_to_use" -> "do zamieszkania"
            "to_completion" -> "do wykończenia"
            "to_renovation" -> "do remontu"
            "block" -> "blok"
            "apartment" -> "apartamentowiec"
            "tenement" -> "kamienica"
            "house" -> "dom"
            "urban" -> "miejskie"
            "gas" -> "gazowe"
            "boiler" -> "piec"
            "wooden" -> "drewniane"
            "plastic" -> "plastikowe"
            "aluminium" -> "aluminiowe"
            else -> raw.replace('_', ' ')
        }
    }
}
