package repository.kufar


import api.KufarApi
import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import core.networkEmptyList
import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.KufarErrorResponse
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

    override fun searchFlats(): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        if (pageCursor == null && filterRepository.currentAppPage > 1) {
            emit(networkEmptyList)
            return@flow
        }

        if (filterRepository.currentAppPage == 1) {
            pageCursor = null
        }

        val filter = filterRepository.lastFilter()
        val metroIds: List<Int>? = filter.metroStations.map { it.metroId }.takeIf { it.isNotEmpty() }

        val city = when (filter.location?.city) {
            null, CityCode.MINSK -> KufarCities.MINSK
            CityCode.BREST -> KufarCities.BREST
            CityCode.GOMEL -> KufarCities.GOMEL
            CityCode.GRODNO -> KufarCities.GRODNO
            CityCode.MOGILEV -> KufarCities.MOGILEV
            CityCode.VITEBSK -> KufarCities.VITEBSK
            else -> KufarCities.MINSK
        }

        val dealType = if (filter.isRentType) AdType.RENT else AdType.SALE

        val params = KufarApi.createQueryParams(
            dealType = dealType,
            priceFull = filter.priceFull,
            pricePerSquare = if (filter.adType == AdType.SALE) filter.pricePerSquare else null,
            metroIds = metroIds,
            onlyOwner = filter.fromOwnerOnly,
            rooms = filter.numberOfRooms,
            cursor = pageCursor,
            geoTag = city,
            sortOption = filter.sortOption
        )

        try {
            val request = api.searchFlats(
                searchId = generateSearchId(),
                queryParams = params
            )

            when (request) {
                is NetworkResponseWrapper.Success -> {
                    pageCursor = request.data.pagination?.pages
                        ?.getOrNull(filterRepository.currentAppPage)
                        ?.token

                    val kufarFlatList = request.data.ads
                        ?.filterNotNull()
                        ?.map { kufarResponseMapper.map(it) }
                        .orEmpty()

                    if (kufarFlatList.isEmpty()) {
                        emit(networkEmptyList)
                    } else {
                        emit(NetworkResponseWrapper.success(kufarFlatList))
                    }
                }

                is NetworkResponseWrapper.Error -> {
                    var parsedError: KufarErrorResponse? = null

                    if (request.ex is ClientRequestException) {
                        val text = request.ex.response.bodyAsText()
                        try {
                            parsedError = Json.decodeFromString(
                                KufarErrorResponse.serializer(), text
                            )
                        } catch (_: Exception) {
                            emit(networkEmptyList)
                        }
                    }

                    emit(
                        NetworkResponseWrapper.error(
                            request.ex, NetworkErrorInfo(
                                platform = FlatPlatform.KUFAR,
                                errorMessages = parsedError?.errorMessages() ?: listOf("Internal server error")
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
//            emit(networkEmptyList)
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