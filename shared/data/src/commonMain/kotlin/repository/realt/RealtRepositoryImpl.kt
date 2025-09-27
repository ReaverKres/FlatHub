package repository.realt


import api.AddressV2
import api.PaginationRequestRealt
import api.RealtApi
import api.RealtGraphqlRequest
import api.SearchData
import api.SortItem
import api.Variables
import api.Where
import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import core.networkEmptyList
import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.extensions.toNullableString
import kotlinx.coroutines.flow.Flow
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

    override fun searchFlats(): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val filter = filterRepository.lastFilter()
        val onlyOwner = if (filter.fromOwnerOnly == true) true else null

        val townUUid = when (filter.location?.city) {
            null, CityCode.MINSK -> RealtCities.MINSK
            CityCode.BREST -> RealtCities.BREST
            CityCode.GOMEL -> RealtCities.GOMEL
            CityCode.GRODNO -> RealtCities.GRODNO
            CityCode.MOGILEV -> RealtCities.MOGILEV
            CityCode.VITEBSK -> RealtCities.VITEBSK
            else -> RealtCities.MINSK
        }

        val category = if (filter.isRentType) 2 else 5
        val priceMax = when {
            filter.priceFull != null -> filter.priceFull.priceTo
            filter.adType == AdType.SALE -> filter.pricePerSquare?.priceTo
            else -> null
        }
        val priceMin = when {
            filter.priceFull != null -> filter.priceFull.priceFrom
            filter.adType == AdType.SALE -> filter.pricePerSquare?.priceFrom
            else -> null
        }

        try {
            val request = api.searchFlats(
                RealtGraphqlRequest(
                    operationName = "searchObjects",
                    variables = Variables(
                        data = SearchData(
                            //TODO Добавить метро
                            where = Where(
                                addressV2 = listOf(AddressV2(townUUid)),
                                category = category,
                                rooms = filter.numberOfRooms?.map { it.toString() },
                                seller = onlyOwner.toString(), // Только собственники
                                priceFrom = priceMin.toNullableString(), // Цена от (можно null если не нужно)
                                priceTo = priceMax.toNullableString(), // Цена до (можно null если не нужно)
                                priceType = "840" // Валюта (840 = USD)
                            ),
                            pagination = PaginationRequestRealt(
                                page = filterRepository.currentAppPage, pageSize = 30
                            ),
                            sort = when (filter.sortOption) {
                                FlatSort.NEWEST_FIRST -> listOf(
                                    SortItem("paymentStatus", "DESC"),
                                    SortItem("priority", "DESC"),
                                    SortItem("raiseDate", "DESC"),
                                    SortItem("updatedAt", "DESC")
                                )
                                FlatSort.CHEAPEST_FIRST -> listOf(
                                    SortItem("price", "ASC")
                                )
                                FlatSort.MOST_EXPENSIVE_FIRST -> listOf(
                                    SortItem("price", "DESC")
                                )
                            }, // Updated sort implementation
                            extraFields = listOf("minPriceAggregation")
                        )
                    ),
                    query = RealtGraphqlRequest.QUERY
                )
            )

            val result: NetworkResponseWrapper<List<AppFlat>> = when (request) {
                is NetworkResponseWrapper.Success -> {
                    val flats = request.data.data?.searchObjects?.body?.results
                        ?.filterNotNull()
                        ?.map { realtResponseMapper.map(it.copy(adType = filter.adType)) }
                        .orEmpty()

                    if (lastEmitList == flats) {
                        networkEmptyList
                    } else {
                        lastEmitList = flats
                        NetworkResponseWrapper.success(flats)
                    }
                }

                is NetworkResponseWrapper.Error -> {
                    NetworkResponseWrapper.error(
                        IllegalStateException(),
                        NetworkErrorInfo(
                            platform = FlatPlatform.REALT,
                            errorMessages = listOf("Что-то пошло не так")
                        )
                    )
                }
            }

            emit(result)
        } catch (e: Exception) {
//            emit(networkEmptyList)
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
        lastEmitList = emptyList()
    }
}