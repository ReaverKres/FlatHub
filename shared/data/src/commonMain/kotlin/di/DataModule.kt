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
import mappers.RealtFlatMapper
import mappers.base.AdditionalParamMapper
import mappers.base.ResponseToEntitiesFlatMapper
import mappers.kufar.KufarDailyFlatMapper
import mappers.kufar.KufarDetailHtmlMapper
import mappers.kufar.KufarFlatMapper
import mappers.onliner.OnlinerDetailHtmlMapper
import mappers.onliner.OnlinerFlatMapper
import maps.TileProviderImpl
import org.koin.dsl.module
import ovh.plrapps.mapcompose.core.TileStreamProvider
import repository.domovita.DomovitaRepository
import repository.domovita.DomovitaRepositoryImpl
import repository.fillter.FilterRepository
import repository.fillter.FilterRepositoryImpl
import repository.fillter.UserMapAreaRepository
import repository.fillter.UserMapAreaRepositoryImpl
import repository.kufar.KufarRepository
import repository.kufar.KufarRepositoryImpl
import repository.mergedrepo.MergedRepository
import repository.mergedrepo.MergedRepositoryImpl
import repository.onliner.OnlinerRepository
import repository.onliner.OnlinerRepositoryImpl
import repository.osm.OsmApiService
import repository.osm.OsmRepository
import repository.osm.OsmRepositoryImpl
import repository.realt.RealtRepository
import repository.realt.RealtRepositoryImpl
import repository.userpreferences.UserPreferencesRepository
import repository.userpreferences.UserPreferencesRepositoryImpl
import server_response.DomovitaListResponse
import server_response.OnlinerListResponse
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse
import server_response.kufar.KufarDailyListResponse
import server_response.kufar.KufarListResponse


val dataModule = module {
    single<ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat>>(
        qualifier = DataQualifiers.KUFAR_FLAT_MAPPER
    ) { KufarFlatMapper() }
    single<ResponseToEntitiesFlatMapper<KufarDailyListResponse.RentalObject, AppFlat>>(
        qualifier = DataQualifiers.KUFAR_DAILY_FLAT_MAPPER
    ) { KufarDailyFlatMapper() }
    single<FilterRepository> { FilterRepositoryImpl(savedFiltersDao = get()) }

    single<UserMapAreaRepository> { UserMapAreaRepositoryImpl(userMapAreasDao = get()) }
    single<OsmApiService> { OsmApiService(httpClient = get(), filterRepository = get()) }
    single<OsmRepository> { OsmRepositoryImpl(osmApiService = get()) }

    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(userPreferencesDao = get()) }

    single<KufarApi> { get<Ktorfit>(qualifier = DataQualifiers.KUFAR_KTORFIT).createKufarApi() }
    single<KufarRepository> {
        KufarRepositoryImpl(
            api = get(),
            ktorClient = get(),
            kufarDetailHtmlMapper = get(qualifier = DataQualifiers.KUFAR_DETAIL_FLAT_MAPPER),
            kufarResponseMapper = get(qualifier = DataQualifiers.KUFAR_FLAT_MAPPER),
            kufarDailyResponseMapper = get(qualifier = DataQualifiers.KUFAR_DAILY_FLAT_MAPPER),
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
    single<AdditionalParamMapper<String, AppFlat>>(qualifier = DataQualifiers.ONLINER_DETAIL_FLAT_MAPPER) { OnlinerDetailHtmlMapper() }
    single<AdditionalParamMapper<String, AppFlat>>(qualifier = DataQualifiers.KUFAR_DETAIL_FLAT_MAPPER) { KufarDetailHtmlMapper() }

    single<OnlinerRepository> {
        OnlinerRepositoryImpl(
            api = get(),
            ktorClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            onlinerResponseMapper = get(qualifier = DataQualifiers.ONLINER_FLAT_MAPPER),
            onlinerDetailHtmlMapper = get(qualifier = DataQualifiers.ONLINER_DETAIL_FLAT_MAPPER),
            filterRepository = get(),
            flatsDao = get(),
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

    single<TileStreamProvider> { TileProviderImpl(httpClient = get()) }
}