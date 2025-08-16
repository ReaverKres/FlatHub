package repository.onliner


import entities.AppFlat
import api.OnlinerApi
import database.FlatsDao
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.OnlinerListResponse

class OnlinerRepositoryImpl(
    private val api: OnlinerApi,
    private val ktorClient: HttpClient,
    private val onlinerResponseMapper: ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>,
    private val onlinerDetailHtmlMapper: AdditionalParamMapper<String, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
    private val connectionMonitor: ConnectionMonitor
) : OnlinerRepository {

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        val filter = filterRepository.lastFilter()
        val params = OnlinerApi.createParams(
            minPrice = filter.priceFrom?.toInt(),
            maxPrice = filter.priceTo?.toInt(),
            metroLines = filter.metroStations.map { it.line.name.lowercase() },
            rooms = filter.numberOfRooms,
            onlyOwner = filter.fromOwnerOnly,
            page = filterRepository.currentAppPage
        )
        val onlinerFlatList = api.searchFlats(params).apartments
            ?.filterNotNull()?.map { onlinerResponseMapper.map(it) }
        emit(onlinerFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        val flatFromList = flatsDao.getAllAsFlow()
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }.first()
        emit(flatFromList)
        if (connectionMonitor.isNetworkAvailable.first() && flatFromList.flatDevInfo.isDetailData.not()) {
            val onlinerDetailFlatHtml = getApartmentHtml(flatFromList.flatDetailUrl)
            val onlinerDetailFlat = onlinerDetailHtmlMapper.map(flatFromList, onlinerDetailFlatHtml)
            emit(onlinerDetailFlat)
        }

    }.flowOn(Dispatchers.IO)

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