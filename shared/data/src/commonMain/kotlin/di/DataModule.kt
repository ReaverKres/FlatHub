package di

import AppFlat
import api.KufarApi
import api.OnlinerApi
import api.createKufarApi
import api.createOnlinerApi
import de.jensklingenberg.ktorfit.Ktorfit
import mappers.KufarFlatMapper
import mappers.OnlinerFlatMapper
import mappers.ResponseToEntitiesFlatMapper
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
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
    single<KufarRepository> { KufarRepositoryImpl(
        api = get(),
        kufarResponseMapper = get(qualifier = DataQualifiers.KUFAR_FLAT_MAPPER)
    ) }

    single<OnlinerApi> { get<Ktorfit>(qualifier = DataQualifiers.ONLINER_KTORFIT).createOnlinerApi() }
    single<ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>>(
        qualifier = DataQualifiers.ONLINER_FLAT_MAPPER
    ) { OnlinerFlatMapper() }
    single<OnlinerRepository> { OnlinerRepositoryImpl(
        api = get(),
        onlinerResponseMapper = get(qualifier = DataQualifiers.ONLINER_FLAT_MAPPER)
    ) }
}
