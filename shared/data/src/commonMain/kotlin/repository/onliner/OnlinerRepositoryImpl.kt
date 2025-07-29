package repository.onliner


import AppFlat
import api.OnlinerApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parsing.ParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mappers.AdditionalParamMapper
import mappers.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import server_request.OnlinerSearchParams
import server_response.OnlinerListResponse

class OnlinerRepositoryImpl(
    private val api: OnlinerApi,
    private val ktorClient: HttpClient,
    private val onlinerResponseMapper: ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>,
    private val onlinerDetailHtmlMapper: AdditionalParamMapper<String, AppFlat>,
    private val filterRepository: FilterRepository
) : OnlinerRepository {

    private val _flatsCache = MutableSharedFlow<List<AppFlat>>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val cashedFlatsFlow: SharedFlow<List<AppFlat>> = _flatsCache

    override fun searchFlats(
        searchParams: OnlinerSearchParams
    ): Flow<List<AppFlat>> = flow {
        val filter = filterRepository.cashedFilterFlow.first()
        val params = OnlinerApi.createParams(
            minPrice = filter.priceFrom?.toInt(),
            maxPrice = filter.priceTo?.toInt(),
            metroLines = filter.metroLine.map { it.name.lowercase() }
        )
        val onlinerFlatList = api.searchFlats(params).apartments
            ?.filterNotNull()?.map { onlinerResponseMapper.map(it) }
        _flatsCache.emit(onlinerFlatList ?: listOf())
        emit(onlinerFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        val flatFromList = _flatsCache
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }.first()
        emit(flatFromList)
        val onlinerDetailFlatHtml = getApartmentHtml(flatFromList.flatDetailUrl)
        val onlinerDetailFlat = onlinerDetailHtmlMapper.map(flatFromList, onlinerDetailFlatHtml)
        emit(onlinerDetailFlat)

    }.flowOn(Dispatchers.IO)

    private suspend fun getApartmentHtml(url: String): String {
        return try {
            ktorClient.get(url).bodyAsText()
        } catch (e: Exception) {
            throw ParseException("Error fetching HTML for $url: ${e.message}")
        }
    }
}