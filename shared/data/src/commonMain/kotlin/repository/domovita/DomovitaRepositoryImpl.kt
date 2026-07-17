package repository.domovita

import api.DomovitaApi
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import listing.core.FeedDelayListBoost
import mappers.base.ResponseToEntitiesFlatMapper
import repository.emitDedupedFlats
import repository.fillter.FilterRepository
import repository.getFlatByIdFromDb
import repository.parseResponseError
import repository.runFlatSearch
import server_response.DomovitaErrorResponse
import server_response.DomovitaListResponse.DomovitaFlat

class DomovitaRepositoryImpl(
    private val api: DomovitaApi,
    private val domovitaResponseMapper: ResponseToEntitiesFlatMapper<DomovitaFlat, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao
) : DomovitaRepository {

    private var lastEmitList: List<AppFlat>? = emptyList()

    override fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int?): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val currentPage = currentPage ?: filterRepository.currentHomePage

        val metroIds: List<Int>? = null
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
            limit = FeedDelayListBoost.apiPageSize(FlatPlatform.DOMOVITA, base = 20),
            priceFull = filter.priceFull,
            pricePerSquare = if (filter.isPricePerSquareNeeded) filter.pricePerSquare else null,
            rooms = filter.numberOfRooms,
            metroIds = metroIds,
            onlyOwner = filter.fromOwnerOnly,
            sortOption = filter.sortOption
        )

        runFlatSearch(
            platform = FlatPlatform.DOMOVITA,
            logTag = "Domovita",
            parseHttpError = { e ->
                parseResponseError(e, DomovitaErrorResponse.serializer()) { it.errorMessages() }
            },
        ) {
            val response = if (filter.adType == AdType.RENT) {
                api.searchRentFlats(request)
            } else {
                api.searchSaleFlats(request)
            }

            val domovitaFlatList =
                response.items.filterNotNull().map { domovitaResponseMapper.map(it) }

            emitDedupedFlats(domovitaFlatList, lastEmitList) { lastEmitList = it }
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> = flow {
        getFlatByIdFromDb(flatId, flatsDao, FlatPlatform.DOMOVITA)
    }.take(1).flowOn(Dispatchers.IO)

    override fun getFlatByIdWithDetails(flatId: Long): Flow<AppFlat> {
        return getFlatById(flatId)
    }

    override fun clearCashedFlats() {
        lastEmitList = emptyList()
    }
}