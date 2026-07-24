package listing.ca.housesigma

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * MD5 request signing for HouseSigma map endpoints.
 * Salt from site bundle (2026-07 probe); rotate if API starts rejecting signatures.
 */
internal object HouseSigmaSigner {
    private const val API_SALT = "ZckdTeV3kGyZd80q"

    fun sign(ts: String, data: Map<String, JsonElement>): String {
        val sorted = data.entries
            .filter { (k, _) -> k != "signature" }
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "$k=${valueForSign(v)}" }
            .lowercase()
        return HouseSigmaMd5.hex(sorted + ts + API_SALT)
    }

    private fun valueForSign(element: JsonElement): String = when (element) {
        is JsonArray -> element.joinToString(",") { valueForSign(it) }
        is JsonObject -> element.toString()
        is JsonPrimitive -> element.contentOrNull ?: element.toString()
        else -> element.toString()
    }
}
