package repository.kufar


import entities.AppFlat
import api.KufarApi
import entities.KufarMetroStations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _flatsCache = MutableStateFlow<List<AppFlat>>(emptyList())
    override val cashedFlatsFlow: SharedFlow<List<AppFlat>> = _flatsCache
    private var lastEmitList: List<AppFlat>? = emptyList()

    private var pageCursor: String? = null

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        if (pageCursor == null && filterRepository.currentAppPage > 1 ) emit(emptyList())
        if(filterRepository.currentAppPage == 1) {
            pageCursor = null
        }
        val filter = filterRepository.cashedFilterFlow.first()
        val params = KufarApi.createQueryParams(
            minPrice = filter.priceFrom,
            maxPrice = filter.priceTo,
            metroIds = filter.metroLine.flatMap { KufarMetroStations.getStationIdsByLine(it) }
                .distinct(),
            onlyOwner = filter.fromOwnerOnly,
            rooms = filter.numberOfRooms,
            cursor = pageCursor
        )
        val kufarFlatList = api.searchFlats(
            searchId = generateSearchId(),
            queryParams = params
        ).also {
            pageCursor = it.pagination?.pages?.getOrNull(filterRepository.currentAppPage)?.token
        }.ads
            ?.filterNotNull()?.map { kufarResponseMapper.map(it) }
        if (lastEmitList == kufarFlatList) {
            _flatsCache.value += listOf()
            emit(listOf())
        } else {
            lastEmitList = kufarFlatList
            _flatsCache.value += (kufarFlatList ?: listOf())
            emit(kufarFlatList ?: listOf())
        }
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

    override fun clearCashedFlats() {
        _flatsCache.value = emptyList()
    }
}