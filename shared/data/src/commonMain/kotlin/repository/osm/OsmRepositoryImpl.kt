package repository.osm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OsmRepositoryImpl(private val osmApiService: OsmApiService): OsmRepository {

    override fun getCityDistricts(name: String): Flow<List<OsmDistricts?>> = flow {
        try {
            val cityId = osmApiService.findCityId(name) ?: run {
                emit(listOf())
                return@flow
            }
            val districts = osmApiService.getCityDistricts(cityId)
            emit(districts)
        } catch (e: Exception) {
            emit(listOf())
        }
    }
}