package di

import AppFlat
import api.KufarApi
import api.OnlinerApi
import api.createKufarApi
import api.createOnlinerApi
import de.jensklingenberg.ktorfit.Ktorfit
import mappers.AdditionalParamMapper
import mappers.KufarFlatMapper
import mappers.ResponseToEntitiesFlatMapper
import mappers.onliner.OnlinerDetailHtmlMapper
import mappers.onliner.OnlinerFlatMapper
import org.koin.dsl.module
import repository.fillter.FilterRepository
import repository.fillter.FilterRepositoryImpl
import repository.kufar.KufarRepository
import repository.kufar.KufarRepositoryImpl
import repository.onliner.OnlinerRepository
import repository.onliner.OnlinerRepositoryImpl
import server_response.KufarListResponse
import server_response.OnlinerListResponse


val dataModule = module {
    single<KufarApi> { get<Ktorfit>(qualifier = DataQualifiers.KUFAR_KTORFIT).createKufarApi() }
    single<ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>>(
        qualifier = DataQualifiers.KUFAR_FLAT_MAPPER
    ) { KufarFlatMapper() }
    single<FilterRepository> { FilterRepositoryImpl() }
    single<KufarRepository> { KufarRepositoryImpl(
        api = get(),
        kufarResponseMapper = get(qualifier = DataQualifiers.KUFAR_FLAT_MAPPER),
        filterRepository = get()
    ) }

    single<OnlinerApi> { get<Ktorfit>(qualifier = DataQualifiers.ONLINER_KTORFIT).createOnlinerApi() }
    single<ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>>(
        qualifier = DataQualifiers.ONLINER_FLAT_MAPPER
    ) { OnlinerFlatMapper() }
    single<AdditionalParamMapper<String, AppFlat>> { OnlinerDetailHtmlMapper() }

    single<OnlinerRepository> { OnlinerRepositoryImpl(
        api = get(),
        ktorClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
        onlinerResponseMapper = get(qualifier = DataQualifiers.ONLINER_FLAT_MAPPER),
        onlinerDetailHtmlMapper = get(),
        filterRepository = get()
    ) }
}
