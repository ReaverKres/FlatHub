package repository.onliner


import api.OnlinerApi
import core.NetworkResponseWrapper
import core.networkEmptyList
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import metro.MetroStationsGeoCatalog
import repository.emitFlats
import repository.fillter.FilterRepository
import repository.getFlatByIdFromDb
import repository.parseResponseError
import repository.runFlatSearch
import server_response.OnlinerListResponse
import server_response.OnlinerSearchErrorResponses
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class OnlinerRepositoryImpl(
    private val api: OnlinerApi,
    private val ktorClient: HttpClient,
    private val onlinerResponseMapper: ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>,
    private val onlinerDetailHtmlMapper: AdditionalParamMapper<String, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
    private val connectionMonitor: ConnectionMonitor
) : OnlinerRepository {

    override fun searchFlats(
        filter: CommonFilterRequestModel,
        currentPage: Int?
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        MetroStationsGeoCatalog.loadIfNeeded()
        val currentPage = currentPage ?: filterRepository.currentHomePage
        val metroLines =
            filter.metroStations.filter { it.selected }.map { it.line.name.lowercase() }.distinct()
        val cityBounds = when (filter.location?.city) {
            null, CityCode.MINSK -> OnlinerCitiesBounds.MINSK
            CityCode.BREST -> OnlinerCitiesBounds.BREST
            CityCode.GOMEL -> OnlinerCitiesBounds.GOMEL
            CityCode.GRODNO -> OnlinerCitiesBounds.GRODNO
            CityCode.MOGILEV -> OnlinerCitiesBounds.MOGILEV
            CityCode.VITEBSK -> OnlinerCitiesBounds.VITEBSK
            else -> OnlinerCitiesBounds.MINSK
        }
        val priceMax = if (filter.priceFull != null) {
            filter.priceFull.priceTo
        } else if (filter.adType == AdType.SALE) {
            filter.pricePerSquare?.priceTo
        } else null
        val priceMin = if (filter.priceFull != null) {
            filter.priceFull.priceFrom
        } else if (filter.adType == AdType.SALE) {
            filter.pricePerSquare?.priceFrom
        } else null

        val params = OnlinerApi.createParams(
            minPrice = priceMin?.roundToInt(),
            maxPrice = priceMax?.roundToInt(),
            metroLines = metroLines,
            onlyOwner = filter.fromOwnerOnly,
            page = currentPage,
            boundsLbLng = cityBounds.southwest.longitude,
            boundsLbLat = cityBounds.southwest.latitude,
            boundsRtLng = cityBounds.northeast.longitude,
            boundsRtLat = cityBounds.northeast.latitude,
            sortOption = filter.sortOption
        )

        runFlatSearch(
            platform = FlatPlatform.ONLINER,
            logTag = "Onliner",
            parseHttpError = { e ->
                parseResponseError(e, OnlinerSearchErrorResponses.serializer()) {
                    it.errorMessages()
                }
            },
        ) {
            val response = if (filter.isRentType) {
                val rentTypes: List<String> = when {
                    filter.roomOnly -> listOf("room")
                    else -> filter.numberOfRooms?.map {
                        if (it == 1) "${it}_room" else "${it}_rooms"
                    } ?: emptyList()
                }
                api.searchRentFlats(params, rentTypes)
            } else {
                val numberOfRooms = filter.numberOfRooms?.toList() ?: emptyList()
                api.searchSaleFlats(params, numberOfRooms)
            }
            val onlinerFlatList = response.apartments
                ?.filterNotNull()?.map { onlinerResponseMapper.map(it) }?.filter {
                    if (filter.isRoomForRent.not() && it.rooms == 0) false else true
                }
            if (onlinerFlatList != null) {
                emitFlats(onlinerFlatList, emptyIfNoItems = false)
            } else {
                emit(networkEmptyList)
            }
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        getFlatByIdFromDb(flatId, flatsDao)
    }.flowOn(Dispatchers.IO)

    override fun getFlatByIdWithDetails(flatId: Long): Flow<AppFlat?> = flow {
        MetroStationsGeoCatalog.loadIfNeeded()
        val flatFromList = getFlatByIdFromDb(flatId, flatsDao)
        if (connectionMonitor.isNetworkAvailable.first() && flatFromList.flatDevInfo.isDetailData.not()) {
            val onlinerDetailFlatHtml = getApartmentHtml(flatFromList.flatDetailUrl)
            val onlinerDetailFlat = onlinerDetailHtmlMapper.map(flatFromList, onlinerDetailFlatHtml)
            emit(onlinerDetailFlat)
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

    private fun OnlinerSearchErrorResponses.errorMessages(): List<String> {
        return errors.flatMap { (field, messages) ->
            messages.map { msg -> "$field: $msg" }
        }
    }
}
