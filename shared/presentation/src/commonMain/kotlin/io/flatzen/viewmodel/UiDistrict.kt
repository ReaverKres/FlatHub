package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.DistrictType
import repository.osm.OsmDistricts

@Immutable
data class UiDistrict(
    val id: Long,
    val isChecked: Boolean,
    val nameEn: String,
    val nameLocal: String,
    val coordinates: List<Coordinates>,
    val type: DistrictType
) {
    companion object {
        fun mapFromModelToUi(areas: List<OsmDistricts?>): List<UiDistrict> {
            return areas.filterNotNull().map {
                UiDistrict(
                    id = it.id,
                    coordinates = it.coordinates,
                    isChecked = it.isChecked,
                    nameEn = it.nameEn,
                    nameLocal = it.nameLocal,
                    type = it.districtType
                )
            }
        }

        fun mapFromUiToModel(areas: List<UiDistrict>?): List<OsmDistricts> {
            return areas?.map {
                OsmDistricts(
                    id = it.id,
                    coordinates = it.coordinates,
                    isChecked = it.isChecked,
                    nameEn = it.nameEn,
                    nameLocal = it.nameLocal,
                    districtType = it.type
                )
            } ?: emptyList()
        }
    }
}
