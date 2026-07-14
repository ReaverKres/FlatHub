package repository.osm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

class OsmRepositoryImpl(private val osmApiService: OsmApiService): OsmRepository {

    override fun getCityDistricts(name: String): Flow<List<OsmDistricts?>> = flow {
        try {
            val cityId = osmApiService.findCityId(name) ?: run {
                emit(listOf())
                return@flow
            }
            val districts = osmApiService.getCityDistricts(cityId)
            emit(districts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(listOf())
        }
    }
}