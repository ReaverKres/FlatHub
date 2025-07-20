package di

import de.jensklingenberg.ktorfit.Ktorfit
import entities.AppFlat
import kz.skiftrade.authdata.api.KufarApi
import mappers.KufarFlatMapper
import mappers.ResponseToEntitiesFlatMapper
import org.koin.dsl.module
import repository.KufarRepository
import repository.KufarRepositoryImpl
import server_response.KufarListResponse


val dataModule = module {
    single<KufarApi> { get<Ktorfit>().createKufarApi() }
    single<ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>> { KufarFlatMapper() }

    single<KufarRepository> { KufarRepositoryImpl(get(), get()) }
}
