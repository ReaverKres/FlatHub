package repository.kufar


import api.KufarApi
import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.KufarListResponse

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val ktorClient: HttpClient,
    private val connectionMonitor: ConnectionMonitor,
    private val kufarDetailHtmlMapper: AdditionalParamMapper<String, AppFlat>,
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
        try {
            val kufarFlatList = api.searchFlats(
                searchId = generateSearchId(),
                queryParams = params
            ).also {
                pageCursor = it.pagination?.pages?.getOrNull(filterRepository.currentAppPage)?.token
            }.ads
                ?.filterNotNull()?.map { kufarResponseMapper.map(it) }
            emit(kufarFlatList ?: listOf())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        val flatFromList =  flatsDao.getAllAsFlow()
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }.first()
        emit(flatFromList)
        if (connectionMonitor.isNetworkAvailable.first() && flatFromList.flatDevInfo.isDetailData.not()) {
            val kufarDetailFlatHtml = getApartmentHtml(flatFromList.flatDetailUrl)
            val kufarDetailFlat = kufarDetailHtmlMapper.map(flatFromList, kufarDetailFlatHtml)
            emit(kufarDetailFlat)
        }
    }

    private fun generateSearchId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }


    private suspend fun getApartmentHtml(url: String): String {
        return try {
            ktorClient.get(url).bodyAsText()
        } catch (e: Exception) {
            throw ParseException("Error fetching HTML for $url: ${e.message}")
        }
    }

    override fun clearCashedFlats() {
    }
}