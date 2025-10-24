package entities

import io.flatzen.commoncomponents.commonentities.FromToRange
import kotlinx.serialization.Serializable

@Serializable
data class CommercialRequestModel(
    val roomRange: FromToRange?
)