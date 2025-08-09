package io.flatzen.viewmodel

import entities.AppFlat
import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import repository.mergedrepo.MergedRepository

@Immutable
data class UiDetailFlat(
    val adId: Long,
    val platform: FlatPlatform,
    val isDetailDataLoaded: Boolean?,
    val flatUrl: String,
    val description: String,
    val imageUrls: List<String>,
    val priceUsd: String,
    val priceByn: String,
    val address: String,
    val district: String?,
    val metroStation: String?,
    val numberOfRooms: String,
    val totalArea: String?,
    val livingArea: String?,
    val kitchenArea: String?,
    val floor: String?,
    val totalFloors: String?,
    val sleepingPlaces: String?,
    val isStudio: Boolean,
    val bathroomType: String?,
    val balcony: String?,
    val repairType: String?,
    val condition: String?,
    val windowDirection: List<String>,
    val buildingImprovements: List<String>,
    val amenities: List<String>,
    val kitchenEquipment: List<String>,
    val prepaymentType: String?,
    val yearBuilt: String?,
    val forWhom: List<String>?,
    val parkingInfo: String?,
    val isOwner: Boolean?,
    val publishedAt: String?,
    val contactInformation: ContactInformationUi
)

@Immutable
data class ContactInformationUi(
    val phones: List<String>?,
    val ownerName: String?
)

sealed interface FlatDetailScreenAction : MviAction {
    data class LoadFlatDetails(val flatPlatform: FlatPlatform, val flatId: Long) : FlatDetailScreenAction
}

@Immutable
data class FlatDetailScreenState(
    val isLoading: Boolean,
    val flat: UiDetailFlat?,
    val error: String?
) : MviState

sealed interface FlatDetailEvents : MviEvent {
    data class FlatLoaded(val flat: LCE<AppFlat>) : FlatDetailEvents
}

class FlatDetailViewModel(
    private val mergedRepository: MergedRepository,
) : BaseMviViewModel<FlatDetailScreenAction, FlatDetailScreenState, FlatDetailEvents, MviEffect>() {

    override fun initialState(): FlatDetailScreenState = FlatDetailScreenState(
        isLoading = false,
        flat = null,
        error = null
    )

    override suspend fun handleIntent(
        action: FlatDetailScreenAction,
        currentState: FlatDetailScreenState
    ): Flow<FlatDetailEvents> {
        return when (action) {
            is FlatDetailScreenAction.LoadFlatDetails -> {
                loadFlatDetails(action.flatPlatform, action.flatId)
            }
        }
    }

    private suspend fun loadFlatDetails(flatPlatform: FlatPlatform, flatId: Long): Flow<FlatDetailEvents> {
        return mergedRepository.getFlatById(flatPlatform, flatId).asLCE().map {
            FlatDetailEvents.FlatLoaded(it)
        }
    }

    override suspend fun reduce(
        event: FlatDetailEvents,
        currentState: FlatDetailScreenState
    ): FlatDetailScreenState {
        return when (event) {
            is FlatDetailEvents.FlatLoaded -> event.flat.process(
                onLoading = {
                    currentState.copy(isLoading = true, error = null)
                },
                onError = { message, _ ->
                    currentState.copy(
                        isLoading = false,
                        error = message,
                        flat = null
                    )
                },
                onSuccess = { appFlat ->
                    val uiFlat = appFlatToUiFlat(appFlat)
                    currentState.copy(
                        isLoading = false,
                        flat = uiFlat,
                        error = null
                    )
                }
            )
        }
    }

    private fun appFlatToUiFlat(appFlat: AppFlat): UiDetailFlat {
        return UiDetailFlat(
            adId = appFlat.adId,
            isDetailDataLoaded = appFlat.flatDevInfo.isDetailLoaded,
            platform = appFlat.flatPlatform,
            flatUrl = appFlat.flatDetailUrl,
            description = appFlat.description.orEmpty(),
            imageUrls = appFlat.imageUrls.orEmpty(),
            priceUsd = formatPrice(appFlat.priceUsd, "USD"),
            priceByn = formatPrice(appFlat.priceByn, "BYN"),
            address = appFlat.address.orEmpty(),
            district = appFlat.district,
            metroStation = appFlat.metroStation,
            numberOfRooms = appFlat.rooms?.let {
                if (appFlat.isStudio == true) "Студия" else "$it"
            } ?: "Не указано",
            totalArea = appFlat.totalArea?.let { formatArea(it) },
            livingArea = appFlat.livingArea?.let { formatArea(it) },
            kitchenArea = appFlat.kitchenArea?.let { formatArea(it) },
            floor = appFlat.floor?.toString(),
            totalFloors = appFlat.totalFloors?.toString(),
            sleepingPlaces = appFlat.sleepingPlaces?.toString(),
            isStudio = appFlat.isStudio ?: false,
            bathroomType = appFlat.bathroomType,
            balcony = appFlat.balcony,
            repairType = appFlat.repairType,
            condition = appFlat.condition,
            windowDirection = appFlat.windowDirections.orEmpty(),
            buildingImprovements = appFlat.buildingImprovements.orEmpty(),
            amenities = appFlat.amenities.orEmpty(),
            kitchenEquipment = appFlat.kitchenEquipment.orEmpty(),
            prepaymentType = appFlat.prepaymentType,
            yearBuilt = appFlat.yearBuilt?.toString(),
            forWhom = appFlat.forWhom,
            parkingInfo = appFlat.parkingInfo,
            isOwner = appFlat.owner,
            publishedAt = appFlat.publishedAtUi,
            contactInformation = ContactInformationUi(
                phones = appFlat.contactInformation?.phones,
                ownerName = appFlat.contactInformation?.ownerName
            )
        )
    }

    private fun formatPrice(price: Double?, currency: String): String {
        return price?.let {
            "${it.toInt()} $currency"
        } ?: "Цена не указана"
    }

    private fun formatArea(area: Double): String {
        return "${area.toInt()} м²"
    }
}