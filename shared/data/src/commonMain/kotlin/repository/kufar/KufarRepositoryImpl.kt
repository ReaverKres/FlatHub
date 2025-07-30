package repository.kufar


import AppFlat
import api.KufarApi
import entities.KufarMetroStations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import server_response.KufarListResponse

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val kufarResponseMapper: ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>,
    private val filterRepository: FilterRepository
) : KufarRepository {

    private val _flatsCache = MutableSharedFlow<List<AppFlat>>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val cashedFlatsFlow: SharedFlow<List<AppFlat>> = _flatsCache

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        val filter = filterRepository.cashedFilterFlow.first()
        val params = KufarApi.createQueryParams(
            minPrice = filter.priceFrom,
            maxPrice = filter.priceTo,
            metroIds = filter.metroLine.flatMap { KufarMetroStations.getStationIdsByLine(it) }.distinct(),
            onlyOwner = filter.fromOwnerOnly,
            rooms = filter.numberOfRooms
        )
        val kufarFlatList = api.searchFlats(
            searchId = generateSearchId(),
            queryParams = params
        ).ads
            ?.filterNotNull()?.map { kufarResponseMapper.map(it) }
        _flatsCache.emit(kufarFlatList ?: listOf())
        emit(kufarFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return _flatsCache
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }
    }

    private fun generateSearchId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }
}