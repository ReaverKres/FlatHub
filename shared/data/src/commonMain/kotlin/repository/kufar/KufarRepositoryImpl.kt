package repository.kufar


import api.KufarApi
import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.KufarListResponse

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val kufarResponseMapper: ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>,
    private val flatsDao: FlatsDao,
    private val filterRepository: FilterRepository
) : KufarRepository {

    private var lastEmitList: List<AppFlat>? = emptyList()

    private var pageCursor: String? = null

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        if (pageCursor == null && filterRepository.currentAppPage > 1 ) emit(emptyList())
        if(filterRepository.currentAppPage == 1) {
            pageCursor = null
        }
        val filter = filterRepository.lastFilter()
        val metroIds: List<Int>? = filter.metroStations.map { it.metroId }.takeIf { it.isNotEmpty() }
        val city = when {
            filter.location?.city == null || filter.location.city == CityCode.MINSK -> {
                KufarCities.MINSK
            }
            filter.location.city == CityCode.BREST -> {
                KufarCities.BREST
            }
            filter.location.city == CityCode.GOMEL -> {
                KufarCities.GOMEL
            }
            filter.location.city == CityCode.GRODNO -> {
                KufarCities.GRODNO
            }
            filter.location.city == CityCode.MOGILEV -> {
                KufarCities.MOGILEV
            }
            filter.location.city == CityCode.VITEBSK -> {
                KufarCities.VITEBSK
            }
            else -> KufarCities.MINSK
        }

        val dealType = if (filter.isRentType) AdType.RENT else AdType.SALE
        val params = KufarApi.createQueryParams(
            dealType = dealType,
            priceFull = filter.priceFull,
            pricePerSquare = if(filter.adType == AdType.SALE) filter.pricePerSquare else null,
            metroIds = metroIds,
            onlyOwner = filter.fromOwnerOnly,
            rooms = filter.numberOfRooms,
            cursor = pageCursor,
            geoTag = city,
            sortOption = filter.sortOption
        )
        val kufarFlatList = api.searchFlats(
            searchId = generateSearchId(),
            queryParams = params
        ).also {
            pageCursor = it.pagination?.pages?.getOrNull(filterRepository.currentAppPage)?.token
        }.ads
            ?.filterNotNull()?.map { kufarResponseMapper.map(it) }
        emit(kufarFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return flatsDao.getAllAsFlow()
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }
            .take(1)
    }

    private fun generateSearchId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }

    override fun clearCashedFlats() {
    }
}