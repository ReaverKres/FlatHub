package repository


import entities.AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kz.skiftrade.authdata.api.KufarApi
import mappers.ResponseToEntitiesFlatMapper
import server_request.KufarSearchParams
import server_response.KufarListResponse

class KufarRepositoryImpl(
    private val api: KufarApi,
    private val kufarResponseMapper: ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>
) : KufarRepository {

    override fun searchFlats(
        searchParams: KufarSearchParams
    ): Flow<List<AppFlat>> = flow {
        api.searchFlats(
            categoryId = searchParams.categoryId,
            currency = searchParams.currency,
            geoTag = searchParams.geoTag,
            language = searchParams.language,
            pageSize = searchParams.pageSize,
            dealType = searchParams.dealType
        ).ads.map { kufarResponseMapper.map(it) }
    }
}