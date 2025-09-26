package repository.mergedrepo

import core.NetworkErrorInfo
import entities.AppFlat

data class MergedFlatResponse(
    val flats: List<AppFlat>,
    val errors: List<NetworkErrorInfo>
    )