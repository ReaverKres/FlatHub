package listing.kr.dabang

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull

/**
 * Dabang detail is login-gated (HTTP 403 anonymous). Keep list [AppFlat] base;
 * phones stay null — UI links out to Dabang.
 */
object DabangDetailMapper {
    fun mergeDetail(base: AppFlat, detailRoot: JsonObject?): AppFlat {
        val marked = base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
        )
        if (detailRoot == null) return marked

        val result = detailRoot["result"].asObjectOrNull() ?: return marked
        val phones = result["phone"].contentOrNull()?.let { listOf(it) }
            ?: result["agentPhone"].contentOrNull()?.let { listOf(it) }
            ?: result["contactList"].asArrayOrNull()
                ?.mapNotNull { it.asObjectOrNull()?.get("phone").contentOrNull() }
                ?.ifEmpty { null }

        val agentName = result["agentName"].contentOrNull()
            ?: result["realtorName"].contentOrNull()

        if (phones == null && agentName == null) return marked

        return marked.copy(
            contactInformation = ContactInformation(
                phones = phones ?: base.contactInformation?.phones,
                ownerName = agentName ?: base.contactInformation?.ownerName,
            ),
        )
    }
}
