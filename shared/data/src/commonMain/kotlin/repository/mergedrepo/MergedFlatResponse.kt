package repository.mergedrepo

import core.NetworkErrorInfo
import entities.AppFlat
import io.flatzen.commoncomponents.localization.LocalizationKeys

data class MergedNetworkErrors(
    val platformErrors: List<NetworkErrorInfo> = emptyList(),
    val generalError: LocalizationKeys? = null,
) {
    val hasDisplayableErrors: Boolean
        get() = platformErrors.any { it.errorMessages.isNotEmpty() } || generalError != null
}

data class MergedFlatResponse(
    val flats: List<AppFlat>,
    val errors: MergedNetworkErrors = MergedNetworkErrors(),
)
