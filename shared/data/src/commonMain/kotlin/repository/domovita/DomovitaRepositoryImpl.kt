package repository.domovita

import api.DomovitaApi
import database.FlatsDao
import io.flatzen.commoncomponents.commonentities.AdType
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.City
import io.flatzen.commoncomponents.commonentities.CityCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.DomovitaListResponse.DomovitaFlat

class DomovitaRepositoryImpl(
    private val api: DomovitaApi,
    private val domovitaResponseMapper: ResponseToEntitiesFlatMapper<DomovitaFlat, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao
) : DomovitaRepository {

    private var lastEmitList: List<AppFlat>? = emptyList()

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        val currentPage = filterRepository.currentAppPage

        // Если не первая страница и нет данных для загрузки, отправляем пустой список
        if (currentPage > 1 && lastEmitList.isNullOrEmpty()) {
            emit(emptyList())
            return@flow
        }

        val filter = filterRepository.lastFilter()
        val metroIds: List<Int>? = null
//            filter.metroStations.map { it.metroId }.takeIf { it.isNotEmpty() }
        val city = when {
            filter.location?.city == null || filter.location.city == CityCode.MINSK -> {
                DomovitaCities.MINSK
            }
            filter.location.city == CityCode.BREST -> {
                DomovitaCities.BREST
            }
            filter.location.city == CityCode.GOMEL -> {
                DomovitaCities.GOMEL
            }
            filter.location.city == CityCode.GRODNO -> {
                DomovitaCities.GRODNO
            }
            filter.location.city == CityCode.MOGILEV -> {
                DomovitaCities.MOGILEV
            }
            filter.location.city == CityCode.VITEBSK -> {
                DomovitaCities.VITEBSK
            }
            else -> DomovitaCities.MINSK
        }

        val request = DomovitaApi.createRequestParams(
            locationSefAlias = city,
            page = currentPage,
            limit = 20,
            priceFull = filter.priceFull,
            pricePerSquare = if(filter.adType == AdType.SALE) filter.pricePerSquare else null,
            rooms = filter.numberOfRooms,
            metroIds = metroIds,
            //TODO Add onlyOwner to request
            onlyOwner = filter.fromOwnerOnly,
            sortOption = filter.sortOption // Added sort option
        )

        try {
            val response = if(filter.adType == AdType.RENT) {
                api.searchRentFlats(request)
            } else {
                api.searchSaleFlats(request)
            }
            var domovitaFlatList =
                response.items.filterNotNull().map { domovitaResponseMapper.map(it) }
            if (filter.fromOwnerOnly == true) {
                domovitaFlatList = domovitaFlatList.filter { it.owner == filter.fromOwnerOnly }
            }

            if(lastEmitList == domovitaFlatList) {
                emit(listOf())
            } else {
                lastEmitList = domovitaFlatList
                emit(domovitaFlatList ?: listOf())
            }
        } catch (e: Exception) {
            // В случае ошибки отправляем пустой список
            emit(emptyList())
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return flatsDao.getAllAsFlow()
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found in Domovita")
            }
            .take(1)
    }

    override fun clearCashedFlats() {
        lastEmitList = emptyList()
    }
}