package repository.realt


import entities.AppFlat
import api.AddressV2
import api.PaginationRequestRealt
import api.RealtApi
import api.RealtGraphqlRequest
import api.SearchData
import api.SortItem
import api.Variables
import api.Where
import database.FlatsDao
import io.flatzen.commoncomponents.extensions.toNullableString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse

class RealtRepositoryImpl(
    private val api: RealtApi,
    private val realtResponseMapper: ResponseToEntitiesFlatMapper<RealtFlatResponse, AppFlat>,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
) : RealtRepository {

    private var lastEmitList: List<AppFlat>? = emptyList()

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        val filter = filterRepository.lastFilter()
        val onlyOwner = if(filter.fromOwnerOnly != null && filter.fromOwnerOnly) {
            true
        } else null
        val realtFlatList = api.searchFlats(
            RealtGraphqlRequest(
                operationName = "searchObjects",
                variables = Variables(
                    data = SearchData(
                        //TODO Добавить метро
                        where = Where(
                            addressV2 = listOf(AddressV2(
                                "4cb07174-7b00-11eb-8943-0cc47adabd66" // Минск
                            )),
                            category = 2,
                            rooms = filter.numberOfRooms?.map { it.toString() },
                            seller = onlyOwner.toString(), // Только собственники
                            priceFrom = filter.priceFrom.toNullableString(), // Цена от (можно null если не нужно)
                            priceTo = filter.priceTo.toNullableString(), // Цена до (можно null если не нужно)
                            priceType = "840" // Валюта (840 = USD)
                        ),
                        pagination = PaginationRequestRealt(
                            page = filterRepository.currentAppPage, pageSize = 30
                        ),
                        sort = listOf(
                            SortItem("paymentStatus", "DESC"),
                            SortItem("priority", "DESC"),
                            SortItem("raiseDate", "DESC"),
                            SortItem("updatedAt", "DESC")
                        ),
                        extraFields = listOf("minPriceAggregation")
                    )
                ),
                query = RealtGraphqlRequest.QUERY
            )
        ).data?.searchObjects?.body?.results?.filterNotNull()?.map { realtResponseMapper.map(it) }
        if(lastEmitList == realtFlatList) {
            emit(listOf())
        } else {
            lastEmitList = realtFlatList
            emit(realtFlatList ?: listOf())
        }
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return flatsDao.getAllAsFlow()
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }
            .take(1)
    }

    override fun clearCashedFlats() {
    }
}