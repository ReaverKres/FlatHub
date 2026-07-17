package listing.kz.olx

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object OlxKzDetailMapper {
    fun mergeInto(base: AppFlat, root: JsonObject, phones: List<String> = emptyList()): AppFlat {
        val item = root["data"]?.jsonObject ?: root
        val remapped = OlxKzFlatMapper.mapSingleOffer(item, base.getAdTypeNonNull())
        val mergedPhones = phones.ifEmpty { null }
            ?: remapped.contactInformation?.phones
            ?: base.contactInformation?.phones
        return remapped.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            savedInFavorites = base.savedInFavorites,
            isViewed = base.isViewed,
            dislike = base.dislike,
            contactInformation = ContactInformation(
                phones = mergedPhones,
                ownerName = remapped.contactInformation?.ownerName
                    ?: base.contactInformation?.ownerName,
            ),
            metroStation = remapped.metroStation ?: base.metroStation,
        )
    }
}
