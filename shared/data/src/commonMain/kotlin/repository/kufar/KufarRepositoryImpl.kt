package repository.kufar


import AppFlat
import api.KufarApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mappers.ResponseToEntitiesFlatMapper
import server_request.KufarSearchParams
import server_response.KufarListResponse

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val kufarResponseMapper: ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>
) : KufarRepository {

    private val _flatsCache = MutableSharedFlow<List<AppFlat>>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val cashedFlatsFlow: SharedFlow<List<AppFlat>> = _flatsCache

    override fun searchFlats(
        searchParams: KufarSearchParams
    ): Flow<List<AppFlat>> = flow {
        val kufarFlatList = api.searchFlats(
            categoryId = searchParams.categoryId,
            currency = searchParams.currency.name.lowercase(),
            geoTag = searchParams.geoTag,
            language = searchParams.language.name.lowercase(),
            pageSize = searchParams.pageSize,
            dealType = searchParams.dealType.name.lowercase(),
            sort = searchParams.sort.paramName,
            searchId = generateSearchId()
        ).ads
            ?.filterNotNull()?.map { kufarResponseMapper.map(it) }
        _flatsCache.emit(kufarFlatList ?: listOf())
        emit(kufarFlatList ?: listOf())
    }

    override fun getFlatById(flatId: Long): Flow<AppFlat> {
        return _flatsCache
            .map { flats ->
                flats.find { it.adId == flatId }
                    ?: throw NoSuchElementException("Flat with id $flatId not found")
            }
    }

    private fun generateSearchId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }
}