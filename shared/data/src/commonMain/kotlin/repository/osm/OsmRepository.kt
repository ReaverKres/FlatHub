package repository.osm

import kotlinx.coroutines.flow.Flow

interface OsmRepository {
    fun getCityDistricts(name: String): Flow<List<OsmDistricts?>>
}