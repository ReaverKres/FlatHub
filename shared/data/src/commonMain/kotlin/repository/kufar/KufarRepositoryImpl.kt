package repository.kufar


import api.KufarApi
import core.NetworkResponseWrapper
import core.networkEmptyList
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.Location
import io.flatzen.commoncomponents.date.toKufarDateDays
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import repository.emitFlats
import repository.fillter.FilterRepository
import repository.getFlatByIdFromDb
import repository.parseResponseError
import repository.runFlatSearch
import server_response.KufarErrorResponse
import server_response.kufar.KufarDailyListQuery
import server_response.kufar.KufarDailyListResponse
import server_response.kufar.KufarListQuery
import server_response.kufar.KufarListResponse
import kotlin.coroutines.cancellation.CancellationException
import kotlin.enums.EnumEntries

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val ktorClient: HttpClient,
    private val connectionMonitor: ConnectionMonitor,
    private val kufarDetailHtmlMapper: AdditionalParamMapper<String, AppFlat>,
    private val kufarResponseMapper: ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>,
    private val kufarDailyResponseMapper: ResponseToEntitiesFlatMapper<KufarDailyListResponse.RentalObject, AppFlat>,
    private val flatsDao: FlatsDao,
    private val filterRepository: FilterRepository
) : KufarRepository {

    private var pageCursor: String? = null

    override fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int?): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {

        val currentPage = currentPage ?: filterRepository.currentHomePage
        if (filter.adType != AdType.DAILY && pageCursor == null && currentPage > 1) {
            emit(networkEmptyList)
            return@flow
        }

        if (currentPage == 1) {
            pageCursor = null
        }

        val metroIds: List<Int>? =
            filter.metroStations.filter { it.selected }.map { it.metroId }
                .takeIf { it.isNotEmpty() }

        val city = when (filter.location?.city) {
            null, CityCode.MINSK -> KufarCities.MINSK
            CityCode.BREST -> KufarCities.BREST
            CityCode.GOMEL -> KufarCities.GOMEL
            CityCode.GRODNO -> KufarCities.GRODNO
            CityCode.MOGILEV -> KufarCities.MOGILEV
            CityCode.VITEBSK -> KufarCities.VITEBSK
            else -> KufarCities.MINSK
        }

        val dealType = filter.adType
        val categoryId = when {
            dealType == AdType.RENT && filter.roomOnly -> 1040
            filter.isCommercial -> 1050
            dealType == AdType.DAILY -> 25010
            else -> 1010
        }

        if (dealType == AdType.DAILY) {
            sendKufarDailyFlatRequest(categoryId, filter, metroIds, city, currentPage)
        } else {
            sendKufarFlatRequest(categoryId, dealType, filter, metroIds, city, currentPage)
        }
    }

    private suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.sendKufarFlatRequest(
        categoryId: Int,
        dealType: AdType,
        filter: CommonFilterRequestModel,
        metroIds: List<Int>?,
        city: String,
        currentPage: Int,
    ) {
        val numberOfRoms = if (filter.roomOnly) null else filter.numberOfRooms
        val params = KufarListQuery.createQueryParams(
            categoryId = categoryId,
            dealType = dealType,
            priceFull = filter.priceFull,
            pricePerSquare = if (filter.isPricePerSquareNeeded) filter.pricePerSquare else null,
            metroIds = metroIds,
            onlyOwner = filter.fromOwnerOnly,
            rooms = numberOfRoms,
            cursor = pageCursor,
            geoTag = city,
            sortOption = filter.sortOption,
            commercialRequestModel = filter.commercial
        )

        runFlatSearch(
            platform = FlatPlatform.KUFAR,
            logTag = "Kufar",
            parseHttpError = { e ->
                parseResponseError(e, KufarErrorResponse.serializer()) { it.errorMessages() }
            },
            messageFallback = { it.message.orEmpty().substringBefore("[") },
        ) {
            val response = api.searchFlats(
                queryParams = params,
                searchId = generateSearchId()
            )

            pageCursor = response.pagination?.pages
                ?.getOrNull(currentPage)
                ?.token

            val kufarFlatList = response.ads
                ?.filterNotNull()
                ?.map { kufarResponseMapper.map(it) }
                .orEmpty()

            emitFlats(kufarFlatList)
        }
    }

    private suspend fun FlowCollector<NetworkResponseWrapper<List<AppFlat>>>.sendKufarDailyFlatRequest(
        categoryId: Int,
        filter: CommonFilterRequestModel,
        metroIds: List<Int>?,
        city: String,
        currentPage: Int
    ) {
        val currentCityCodes: EnumEntries<CityCode> = CityCode.entries
        val currentCityName = currentCityCodes.find { it == filter.location?.city }?.let {
            Location.mapCityCodeToDomainName(it)
        }
        val numberOfRoms = if (filter.roomOnly) null else filter.numberOfRooms
        val params = KufarDailyListQuery.createQueryParams(
            categoryId = categoryId,
            page = currentPage,
            metroIds = metroIds,
            rooms = numberOfRoms,
            priceFull = filter.priceFull,
            geoTag = city,
            sortOption = filter.sortOption,
            dateFrom = filter.bookingDatesFilter?.dateFrom?.toKufarDateDays(),
            dateTo = filter.bookingDatesFilter?.dateTo?.toKufarDateDays()
        )

        runFlatSearch(
            platform = FlatPlatform.KUFAR,
            logTag = "Kufar daily",
            parseHttpError = { e ->
                parseResponseError(e, KufarErrorResponse.serializer()) { it.errorMessages() }
            },
            messageFallback = { it.message.orEmpty().substringBefore("[") },
        ) {
            val response = api.searchFlatsDaily(queryParams = params)

            val kufarUsualFlatList = response.rentalObjects
                ?.filterNotNull()
                ?.map { mapDailyFlat(it, currentCityName) }
                .orEmpty()
            val kufarPoleFlatList = response.polePosition
                ?.filterNotNull()
                ?.map { mapDailyFlat(it, currentCityName) }
                .orEmpty()
            val kufarFlatList = kufarUsualFlatList + kufarPoleFlatList

            if (kufarFlatList.isEmpty() || (response.paginator?.page ?: -1) >=
                (response.paginator?.pages ?: -1)
            ) {
                emit(networkEmptyList)
            } else {
                emitFlats(kufarFlatList, emptyIfNoItems = false)
            }
        }
    }

    private fun mapDailyFlat(
        rentalObject: KufarDailyListResponse.RentalObject,
        currentCityName: String?
    ): AppFlat = kufarDailyResponseMapper.map(
        rentalObject.copy(
            appCity = currentCityName,
            currentPage = filterRepository.currentHomePage
        )
    )

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        getFlatByIdFromDb(flatId, flatsDao)
    }.flowOn(Dispatchers.IO)

    override fun getFlatByIdWithDetails(flatId: Long): Flow<AppFlat?> = flow {
        val flatFromDb = getFlatByIdFromDb(flatId, flatsDao)
        if (connectionMonitor.isNetworkAvailable.first() && flatFromDb.flatDevInfo.isDetailData.not()) {
            val kufarDetailFlatHtml = getApartmentHtml(flatFromDb.flatDetailUrl)
            val kufarDetailFlat = kufarDetailHtmlMapper.map(flatFromDb, kufarDetailFlatHtml)
            emit(kufarDetailFlat)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)


    private suspend fun getApartmentHtml(url: String): String {
        return try {
            ktorClient.get(url).bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ParseException("Error fetching HTML for $url: ${e.message}")
        }
    }

    override fun clearCashedFlats() {
    }

    private fun generateSearchId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }
}
