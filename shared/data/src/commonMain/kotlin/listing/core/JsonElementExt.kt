package listing.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Safe cast — kotlinx `.jsonObject` throws on [kotlinx.serialization.json.JsonNull]. */
fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

fun JsonElement?.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

fun JsonElement?.contentOrNull(): String? = asPrimitiveOrNull()?.contentOrNull

fun JsonElement?.longOrNull(): Long? = asPrimitiveOrNull()?.longOrNull

fun JsonElement?.doubleOrNull(): Double? = asPrimitiveOrNull()?.doubleOrNull
    ?: contentOrNull()?.replace(',', '.')?.toDoubleOrNull()
