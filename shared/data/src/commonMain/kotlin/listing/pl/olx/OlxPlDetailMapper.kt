package listing.pl.olx

import entities.AppFlat
import entities.FlatDevInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object OlxPlDetailMapper {
    /**
     * Remaps offer JSON into [base], marking detail as loaded.
     * Anonymous OLX traffic typically still omits phone numbers.
     */
    fun mergeInto(base: AppFlat, root: JsonObject): AppFlat {
        val item = root["data"]?.jsonObject ?: root
        val remapped = OlxPlFlatMapper.mapSingleOffer(item, base.getAdTypeNonNull())
        return remapped.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            savedInFavorites = base.savedInFavorites,
            isViewed = base.isViewed,
            dislike = base.dislike,
            contactInformation = remapped.contactInformation?.let { contact ->
                contact.copy(
                    phones = contact.phones ?: base.contactInformation?.phones,
                    ownerName = contact.ownerName ?: base.contactInformation?.ownerName,
                )
            } ?: base.contactInformation,
            metroStation = remapped.metroStation ?: base.metroStation,
        )
    }
}
