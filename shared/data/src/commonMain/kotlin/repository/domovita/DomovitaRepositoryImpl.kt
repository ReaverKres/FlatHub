package repository.domovita

import api.DomovitaApi
import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import core.networkEmptyList
import database.FlatsDao
import io.flatzen.commoncomponents.commonentities.AdType
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.serialization.json.Json
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.getFlatByIdFromDb
import server_response.DomovitaErrorResponse
import server_response.DomovitaListResponse.DomovitaFlat

class DomovitaRepositoryImpl(
    private val api: DomovitaApi,
    private val domovitaResponseMapper: ResponseToEntitiesFlatMapper<DomovitaFlat, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao
) : DomovitaRepository {

    private var lastEmitList: List<AppFlat>? = emptyList()

    override fun searchFlats(): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val currentPage = filterRepository.currentAppPage

        // Если не первая страница и нет данных для загрузки, отправляем пустой список
//        if (currentPage > 1 && lastEmitList.isNullOrEmpty()) {
//            emit(networkEmptyList)
//            return@flow
//        }

        val filter = filterRepository.lastFilter()
        val metroIds: List<Int>? = null
//            filter.metroStations.map { it.metroId }.takeIf { it.isNotEmpty() }
        val city = when (filter.location?.city) {
            null, CityCode.MINSK -> DomovitaCities.MINSK
            CityCode.BREST -> DomovitaCities.BREST
            CityCode.GOMEL -> DomovitaCities.GOMEL
            CityCode.GRODNO -> DomovitaCities.GRODNO
            CityCode.MOGILEV -> DomovitaCities.MOGILEV
            CityCode.VITEBSK -> DomovitaCities.VITEBSK
            else -> DomovitaCities.MINSK
        }

        val request = DomovitaApi.createRequestParams(
            locationSefAlias = city,
            page = currentPage,
            limit = 20,
            priceFull = filter.priceFull,
            pricePerSquare = if (filter.isPricePerSquareNeeded) filter.pricePerSquare else null,
            rooms = filter.numberOfRooms,
            metroIds = metroIds,
            //TODO Add onlyOwner to request
            onlyOwner = filter.fromOwnerOnly,
            sortOption = filter.sortOption // Added sort option
        )

        try {
            val request = if (filter.adType == AdType.RENT) {
                api.searchRentFlats(request)
            } else {
                api.searchSaleFlats(request)
            }

            when (request) {
                is NetworkResponseWrapper.Success -> {
                    val domovitaFlatList =
                        request.data.items.filterNotNull().map { domovitaResponseMapper.map(it) }

                    if (lastEmitList == domovitaFlatList) {
                        emit(networkEmptyList)
                    } else {
                        lastEmitList = domovitaFlatList
                        emit(NetworkResponseWrapper.success(domovitaFlatList))
                    }
                }

                is NetworkResponseWrapper.Error -> {
                    var parsedError: DomovitaErrorResponse? = null

                    if (request.ex is io.ktor.client.plugins.ClientRequestException ||
                        request.ex is io.ktor.client.plugins.ServerResponseException
                    ) {
                        val text = request.ex.response.bodyAsText()
                        try {
                            parsedError = Json.decodeFromString(
                                DomovitaErrorResponse.serializer(),
                                text
                            )
                        } catch (_: Exception) {
                            emit(networkEmptyList)
                        }
                    }

                    emit(
                        NetworkResponseWrapper.error(
                            request.ex, NetworkErrorInfo(
                                platform = FlatPlatform.DOMOVITA,
                                errorMessages = parsedError?.errorMessages() ?: listOf()
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
//            // В случае ошибки отправляем пустой список
//            emit(networkEmptyList)
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        getFlatByIdFromDb(flatId, flatsDao)
    }.take(1).flowOn(Dispatchers.IO)

    override fun getFlatByIdWithDetails(flatId: Long): Flow<AppFlat> {
        return getFlatById(flatId)
    }

    override fun clearCashedFlats() {
        lastEmitList = emptyList()
    }
}