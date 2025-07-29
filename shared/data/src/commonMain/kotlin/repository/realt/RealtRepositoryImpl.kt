package repository.realt


import AppFlat
import api.AddressV2
import api.PaginationRequestRealt
import api.RealtApi
import api.RealtGraphqlRequest
import api.SearchData
import api.SortItem
import api.Variables
import api.Where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mappers.base.ResponseToEntitiesFlatMapper
import repository.fillter.FilterRepository
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse

class RealtRepositoryImpl(
    private val api: RealtApi,
    private val realtResponseMapper: ResponseToEntitiesFlatMapper<RealtFlatResponse, AppFlat>,
    private val filterRepository: FilterRepository
) : RealtRepository {

    private val _flatsCache = MutableSharedFlow<List<AppFlat>>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val cashedFlatsFlow: SharedFlow<List<AppFlat>> = _flatsCache

    override fun searchFlats(): Flow<List<AppFlat>> = flow {
        val realtFlatList = api.searchFlats(
            RealtGraphqlRequest(
                operationName = "searchObjects",
                variables = Variables(
                    data = SearchData(
                        where = Where(
                            addressV2 = listOf(AddressV2(
                                "4cb07174-7b00-11eb-8943-0cc47adabd66"
                            )),
                            category = 2
                        ),
                        pagination = PaginationRequestRealt(page = 1, pageSize = 30),
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
        _flatsCache.emit(realtFlatList ?: listOf())
        emit(realtFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return _flatsCache
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }
    }
}