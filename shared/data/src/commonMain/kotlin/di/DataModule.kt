package di

import api.DomovitaApi
import api.KufarApi
import api.OnlinerApi
import api.RealtApi
import api.createDomovitaApi
import api.createKufarApi
import api.createOnlinerApi
import api.createRealtApi
import de.jensklingenberg.ktorfit.Ktorfit
import entities.AppFlat
import mappers.DomovitaFlatMapper
import mappers.KufarFlatMapper
import mappers.RealtFlatMapper
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import mappers.onliner.OnlinerDetailHtmlMapper
import mappers.onliner.OnlinerFlatMapper
import maps.CachedOsmTileProvider
import org.koin.dsl.module
import ovh.plrapps.mapcompose.core.TileStreamProvider
import repository.domovita.DomovitaRepository
import repository.domovita.DomovitaRepositoryImpl
import repository.fillter.FilterRepository
import repository.fillter.FilterRepositoryImpl
import repository.kufar.KufarRepository
import repository.kufar.KufarRepositoryImpl
import repository.mergedrepo.MergedRepository
import repository.mergedrepo.MergedRepositoryImpl
import repository.onliner.OnlinerRepository
import repository.onliner.OnlinerRepositoryImpl
import repository.realt.RealtRepository
import repository.realt.RealtRepositoryImpl
import server_response.DomovitaListResponse
import server_response.KufarListResponse
import server_response.OnlinerListResponse
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse


val dataModule = module {
    single<KufarApi> { get<Ktorfit>(qualifier = DataQualifiers.KUFAR_KTORFIT).createKufarApi() }
    single<ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>>(
        qualifier = DataQualifiers.KUFAR_FLAT_MAPPER
    ) { KufarFlatMapper() }
    single<FilterRepository> { FilterRepositoryImpl(savedFiltersDao = get()) }
    single<KufarRepository> {
        KufarRepositoryImpl(
            api = get(),
            kufarResponseMapper = get(qualifier = DataQualifiers.KUFAR_FLAT_MAPPER),
            filterRepository = get(),
            flatsDao = get()
        )
    }

    single<MergedRepository> {
        MergedRepositoryImpl(
            kufarRepository = get(),
            onlinerRepository = get(),
            realtRepository = get(),
            domovitaRepository = get(),
            filterRepository = get(),
            flatsDao = get()
        )
    }

    single<OnlinerApi> { get<Ktorfit>(qualifier = DataQualifiers.ONLINER_KTORFIT).createOnlinerApi() }
    single<ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat>>(
        qualifier = DataQualifiers.ONLINER_FLAT_MAPPER
    ) { OnlinerFlatMapper() }
    single<AdditionalParamMapper<String, AppFlat>> { OnlinerDetailHtmlMapper() }

    single<OnlinerRepository> {
        OnlinerRepositoryImpl(
            api = get(),
            ktorClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            onlinerResponseMapper = get(qualifier = DataQualifiers.ONLINER_FLAT_MAPPER),
            onlinerDetailHtmlMapper = get(),
            filterRepository = get(),
            flatsDao = get(),
            connectionMonitor = get()
        )
    }

    single<RealtApi> { get<Ktorfit>(qualifier = DataQualifiers.REALT_KTORFIT).createRealtApi() }
    single<ResponseToEntitiesFlatMapper<RealtFlatResponse, AppFlat>>(
        qualifier = DataQualifiers.REALT_FLAT_MAPPER
    ) {
        RealtFlatMapper()
    }
    single<RealtRepository> {
        RealtRepositoryImpl(
            api = get(),
            realtResponseMapper = get(qualifier = DataQualifiers.REALT_FLAT_MAPPER),
            filterRepository = get(),
            flatsDao = get()
        )
    }

    single<DomovitaApi> { get<Ktorfit>(qualifier = DataQualifiers.DOMOVITA_KTORFIT).createDomovitaApi() }
    single<ResponseToEntitiesFlatMapper<DomovitaListResponse.DomovitaFlat, AppFlat>>(
        qualifier = DataQualifiers.DOMOVITA_FLAT_MAPPER
    ) {
        DomovitaFlatMapper()
    }
    single<DomovitaRepository> {
        DomovitaRepositoryImpl(
            api = get(),
            domovitaResponseMapper = get(qualifier = DataQualifiers.DOMOVITA_FLAT_MAPPER),
            filterRepository = get(),
            flatsDao = get()
        )
    }

    single<TileStreamProvider> { CachedOsmTileProvider(httpClient = get()) }
}
