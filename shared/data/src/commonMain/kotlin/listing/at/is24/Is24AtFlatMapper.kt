package listing.at.is24

import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.de.is24.Is24FlatMapper

/**
 * IS24 AT — reuses DE mobile JSON mapper; `.at` detail URLs.
 * See tmp/at/api/is24/NOTES.md.
 */
object Is24AtFlatMapper {
    private const val DETAIL_BASE = "https://www.immobilienscout24.at/expose/"

    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> =
        Is24FlatMapper.mapSearch(root, adType).map { it.atListing() }

    fun mergeDetail(base: AppFlat, detail: JsonObject): AppFlat {
        val merged = Is24FlatMapper.mergeDetail(
            base.copy(flatPlatform = FlatPlatform.IS24_AT),
            detail,
        )
        val clickOut = detail["contact"].asObjectOrNull()?.get("clickOutUrl").contentOrNull()
        return merged.copy(
            flatPlatform = FlatPlatform.IS24_AT,
            flatDetailUrl = clickOut
                ?: merged.flatDetailUrl?.replace("immobilienscout24.de", "immobilienscout24.at")
                ?: "$DETAIL_BASE${base.adId}",
        )
    }

    fun listStub(adId: Long, adType: AdType = AdType.RENT): AppFlat =
        Is24FlatMapper.listStub(adId, adType).atListing()

    private fun AppFlat.atListing(): AppFlat = copy(
        flatPlatform = FlatPlatform.IS24_AT,
        flatDetailUrl = "$DETAIL_BASE$adId",
    )
}
