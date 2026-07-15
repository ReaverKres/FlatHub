package entities

import kotlinx.serialization.Serializable

@Serializable
data class MetroStationGeoDto(
    val name: String,
    val coordinates: List<Double>,
) {
    val latitude: Double get() = coordinates[0]
    val longitude: Double get() = coordinates[1]
}
