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
import org.koin.dsl.module
import repository.kufar.KufarRepository
import repository.kufar.KufarRepositoryImpl
import repository.onliner.OnlinerRepository
import repository.onliner.OnlinerRepositoryImpl
import server_response.KufarListResponse
import server_response.OnlinerListResponse


val dataModule = module {
    single<KufarApi> { get<Ktorfit>().createKufarApi() }
    single<ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>> { KufarFlatMapper() }

    single<KufarRepository> { KufarRepositoryImpl(get(), get()) }

    single<OnlinerApi> { get<Ktorfit>().createOnlinerApi() }
    single<ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>> { OnlinerFlatMapper() }

    single<OnlinerRepository> { OnlinerRepositoryImpl(get(), get()) }
}
