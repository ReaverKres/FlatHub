package entities

import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.serialization.Serializable

@Serializable
data class MapAreas(
    val id: String,
    val coordinates: List<Coordinates>,
    val isActive: Boolean,
    val name: String
)
