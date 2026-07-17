package di

import api.DomovitaApi
import api.KufarApi
import api.OnlinerApi
import api.RealtApi
import api.ReferralsApi
import api.SubscriptionsApi
import api.createDomovitaApi
import api.createKufarApi
import api.createOnlinerApi
import api.createRealtApi
import api.createReferralsApi
import api.createSubscriptionsApi
import de.jensklingenberg.ktorfit.Ktorfit
import entities.AppFlat
import io.flatzen.firebase.ConfigFieldsChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import listing.ae.dubizzle.DubizzleApiClient
import listing.ae.dubizzle.DubizzleListingSource
import listing.ae.opensooq.OpenSooqApiClient
import listing.ae.opensooq.OpenSooqListingSource
import listing.ae.propertyfinder.PropertyFinderApiClient
import listing.ae.propertyfinder.PropertyFinderListingSource
import listing.by.byListingSources
import listing.core.CoordEnrichState
import listing.core.CoordEnricher
import listing.core.ListingSourceRegistry
import listing.core.RemoteListingPlatformConfig
import listing.de.immowelt.ImmoweltApiClient
import listing.de.immowelt.ImmoweltListingSource
import listing.de.is24.Is24ApiClient
import listing.de.is24.Is24ListingSource
import listing.de.kleinanzeigen.KleinanzeigenApiClient
import listing.de.kleinanzeigen.KleinanzeigenListingSource
import listing.es.fotocasa.FotocasaApiClient
import listing.es.fotocasa.FotocasaListingSource
import listing.es.pisos.PisosApiClient
import listing.es.pisos.PisosListingSource
import listing.ge.binebi.BinebiApiClient
import listing.ge.binebi.BinebiListingSource
import listing.ge.livo.LivoApiClient
import listing.ge.livo.LivoListingSource
import listing.ge.ss.SsApiClient
import listing.ge.ss.SsListingSource
import listing.kz.kn.KnApiClient
import listing.kz.kn.KnListingSource
import listing.kz.krisha.KrishaApiClient
import listing.kz.krisha.KrishaListingSource
import listing.kz.olx.OlxKzApiClient
import listing.kz.olx.OlxKzListingSource
import listing.pl.gratka.GratkaApiClient
import listing.pl.gratka.GratkaListingSource
import listing.pl.morizon.MorizonApiClient
import listing.pl.morizon.MorizonListingSource
import listing.pl.olx.OlxPlApiClient
import listing.pl.olx.OlxPlListingSource
import listing.pl.otodom.OtodomApiClient
import listing.pl.otodom.OtodomListingSource
import listing.th.livinginsider.LivinginsiderApiClient
import listing.th.livinginsider.LivinginsiderListingSource
import listing.th.propertyhub.PropertyHubApiClient
import listing.th.propertyhub.PropertyHubListingSource
import listing.th.renthub.RentHubApiClient
import listing.th.renthub.RentHubListingSource
import listing.tr.emlakjet.EmlakjetApiClient
import listing.tr.emlakjet.EmlakjetListingSource
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
import org.koin.core.qualifier.named
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
import repository.referrals.ReferralsRepository
import repository.referrals.ReferralsRepositoryImpl
import repository.subscriptions.SubscriptionsRepository
import repository.subscriptions.SubscriptionsRepositoryImpl
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
    single<OsmRepository> {
        OsmRepositoryImpl(
            osmApiService = get(),
            filterRepository = get(),
        )
    }

    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(userPreferencesDao = get()) }

    single<KufarApi> { get<Ktorfit>(qualifier = DataQualifiers.KUFAR_KTORFIT).createKufarApi() }
    single<KufarRepository> {
        KufarRepositoryImpl(
            api = get(),
            ktorClient = get(),
            connectionMonitor = get(),
            kufarDetailHtmlMapper = get(qualifier = DataQualifiers.KUFAR_DETAIL_FLAT_MAPPER),
            kufarResponseMapper = get(qualifier = DataQualifiers.KUFAR_FLAT_MAPPER),
            kufarDailyResponseMapper = get(qualifier = DataQualifiers.KUFAR_DAILY_FLAT_MAPPER),
            filterRepository = get(),
            flatsDao = get()
        )
    }

    single {
        OtodomApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { OtodomListingSource(api = get(), flatsDao = get()) }
    single {
        OlxPlApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { OlxPlListingSource(api = get(), flatsDao = get()) }
    single {
        GratkaApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { GratkaListingSource(api = get(), flatsDao = get()) }
    single {
        MorizonApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { MorizonListingSource(api = get(), flatsDao = get()) }

    single {
        SsApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { SsListingSource(api = get(), flatsDao = get()) }
    single {
        LivoApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { LivoListingSource(api = get(), flatsDao = get()) }
    single {
        BinebiApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { BinebiListingSource(api = get(), flatsDao = get()) }

    single { KrishaApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { KrishaListingSource(api = get(), flatsDao = get()) }
    single {
        OlxKzApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { OlxKzListingSource(api = get(), flatsDao = get()) }
    single {
        KnApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { KnListingSource(api = get(), flatsDao = get()) }

    single { PisosApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { PisosListingSource(api = get(), flatsDao = get()) }
    single {
        FotocasaApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { FotocasaListingSource(api = get(), flatsDao = get()) }

    single {
        Is24ApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { Is24ListingSource(api = get(), flatsDao = get()) }
    single { ImmoweltApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { ImmoweltListingSource(api = get(), flatsDao = get()) }
    single { KleinanzeigenApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { KleinanzeigenListingSource(api = get(), flatsDao = get()) }

    single { EmlakjetApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { EmlakjetListingSource(api = get(), flatsDao = get()) }

    single { PropertyFinderApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { PropertyFinderListingSource(api = get(), flatsDao = get()) }
    single {
        DubizzleApiClient(
            httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT),
            json = get(named("defaultJson")),
        )
    }
    single { DubizzleListingSource(api = get(), flatsDao = get()) }
    single { OpenSooqApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { OpenSooqListingSource(api = get(), flatsDao = get()) }

    single { PropertyHubApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { PropertyHubListingSource(api = get(), flatsDao = get()) }
    single { LivinginsiderApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { LivinginsiderListingSource(api = get(), flatsDao = get()) }
    single { RentHubApiClient(httpClient = get(qualifier = DataQualifiers.HTML_KTOR_CLIENT)) }
    single { RentHubListingSource(api = get(), flatsDao = get()) }

    single {
        ListingSourceRegistry(
            sources = byListingSources(
                kufar = get(),
                onliner = get(),
                realt = get(),
                domovita = get(),
            ) + listOf(
                get<OtodomListingSource>(),
                get<OlxPlListingSource>(),
                get<GratkaListingSource>(),
                get<MorizonListingSource>(),
                get<SsListingSource>(),
                get<LivoListingSource>(),
                get<BinebiListingSource>(),
                get<KrishaListingSource>(),
                get<OlxKzListingSource>(),
                get<KnListingSource>(),
                get<PisosListingSource>(),
                get<FotocasaListingSource>(),
                get<Is24ListingSource>(),
                get<ImmoweltListingSource>(),
                get<KleinanzeigenListingSource>(),
                get<EmlakjetListingSource>(),
                get<PropertyFinderListingSource>(),
                get<DubizzleListingSource>(),
                get<OpenSooqListingSource>(),
                get<PropertyHubListingSource>(),
                get<LivinginsiderListingSource>(),
                get<RentHubListingSource>(),
            ),
            platformConfig = RemoteListingPlatformConfig(get<ConfigFieldsChecker>()),
        )
    }

    single { CoordEnrichState() }
    single {
        CoordEnricher(
            flatsDao = get(),
            registry = get(),
            state = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }

    single<MergedRepository> {
        MergedRepositoryImpl(
            listingSourceRegistry = get(),
            filterRepository = get(),
            flatsDao = get(),
            connectionMonitor = get(),
            coordEnricher = get(),
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

    single<SubscriptionsApi> { get<Ktorfit>(qualifier = DataQualifiers.SUBSCRIPTIONS_KTORFIT).createSubscriptionsApi() }
    single<SubscriptionsRepository> { SubscriptionsRepositoryImpl(api = get()) }

    // Referrals
    single<ReferralsApi> { get<Ktorfit>(qualifier = DataQualifiers.SUBSCRIPTIONS_KTORFIT).createReferralsApi() }
    single<ReferralsRepository> { ReferralsRepositoryImpl(get()) }

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

    single<TileStreamProvider> {
        TileProviderImpl(
            httpClient = get(),
            diskCache = get(),
        )
    }
}