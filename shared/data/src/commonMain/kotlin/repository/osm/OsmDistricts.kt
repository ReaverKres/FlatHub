package repository.osm

import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.serialization.Serializable

@Serializable
data class OsmDistricts(
    val id: Long,
    val nameEn: String,
    val nameLocal: String,
    val coordinates: List<Coordinates>
)