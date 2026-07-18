package listing.by

import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import listing.core.FlatsRepositoryListingSource
import listing.core.ListingSource
import listing.core.SourceCapabilities
import repository.domovita.DomovitaRepository
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository

fun byListingSources(
    kufar: KufarRepository,
    onliner: OnlinerRepository,
    realt: RealtRepository,
    domovita: DomovitaRepository,
): List<ListingSource> = listOf(
    FlatsRepositoryListingSource(
        platform = FlatPlatform.KUFAR,
        country = CountryCode.BY,
        capabilities = SourceCapabilities(
            supportsRent = true,
            supportsSale = true,
            supportsDaily = true,
            supportsRoom = true,
            supportsCommercial = true,
            supportsCommercialPropertyTypes = true,
        ),
        repository = kufar,
    ),
    FlatsRepositoryListingSource(
        platform = FlatPlatform.ONLINER,
        country = CountryCode.BY,
        capabilities = SourceCapabilities(
            supportsRent = true,
            supportsSale = true,
            supportsDaily = false,
            supportsRoom = true,
            supportsCommercial = false,
        ),
        repository = onliner,
    ),
    FlatsRepositoryListingSource(
        platform = FlatPlatform.REALT,
        country = CountryCode.BY,
        capabilities = SourceCapabilities(
            supportsRent = true,
            supportsSale = true,
            supportsDaily = true,
            supportsRoom = false,
            supportsCommercial = true,
            supportsCommercialPropertyTypes = true,
        ),
        repository = realt,
    ),
    FlatsRepositoryListingSource(
        platform = FlatPlatform.DOMOVITA,
        country = CountryCode.BY,
        capabilities = SourceCapabilities(
            supportsRent = true,
            supportsSale = true,
            supportsDaily = false,
            supportsRoom = false,
            supportsCommercial = false,
        ),
        repository = domovita,
    ),
)
