package repository.osm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import kotlin.coroutines.cancellation.CancellationException

class OsmRepositoryImpl(
    private val osmApiService: OsmApiService,
    private val filterRepository: FilterRepository,
) : OsmRepository {

    override fun getCityDistricts(name: String): Flow<List<OsmDistricts?>> = flow {
        try {
            CityDistrictsCatalog.loadIfNeeded()
            val local = CityDistrictsCatalog.districtsForCity(name)
            if (local.isNotEmpty()) {
                emit(applyCheckedState(local))
                return@flow
            }

            val cityId = osmApiService.findCityId(name) ?: run {
                emit(emptyList())
                return@flow
            }
            val districts = osmApiService.getCityDistricts(cityId)
            emit(districts)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    private fun applyCheckedState(districts: List<OsmDistricts>): List<OsmDistricts> {
        val checkedIds = filterRepository.lastFilter()
            .districtsArea
            .filter { it.isChecked }
            .map { it.id }
            .toSet()
        if (checkedIds.isEmpty()) return districts
        return districts.map { it.copy(isChecked = it.id in checkedIds) }
    }
}
