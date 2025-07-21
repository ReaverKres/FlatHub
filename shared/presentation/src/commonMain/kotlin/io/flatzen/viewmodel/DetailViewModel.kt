package io.flatzen.viewmodel

import AppFlat
import androidx.compose.runtime.Immutable
import entities.BalconyType
import entities.BathroomType
import entities.BuildingImprovement
import entities.PrepaymentType
import entities.RepairType
import entities.WindowDirection
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
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository

@Immutable
data class UiDetailFlat(
    val adId: Long,
    val platform: FlatPlatform,
    val flatUrl: String,
    val title: String,
    val additionalParams: UiAdditionalParams?,
    val description: String,
    val imageUrls: List<String>,
    val priceUsd: String,
    val priceByn: String,
    val address: String,
    val district: String,
    val numberOfRooms: Int,
    val totalArea: String?,
    val floor: String?,
    val totalFloors: String?,
    val sleepingPlaces: String?,
    val isStudio: Boolean,
    val bathroomType: String?,
    val balconyType: String?,
    val repairType: String?,
    val windowDirection: List<String>,
    val buildingImprovement: List<String>,
    val prepaymentType: String?,
    val yearBuilt: String?
)

@Immutable
data class UiAdditionalParams(
    val forWhom: List<String>?,
    val hasWashingMachine: Boolean,
    val hasStove: Boolean,
    val hasMicrowave: Boolean,
    val hasWifi: Boolean,
    val hasFurniture: Boolean,
    val hasConditioner: Boolean
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
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository
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
        return when (flatPlatform) {
            FlatPlatform.KUFAR -> {
                kufarRepository.getFlatById(flatId).asLCE().map {
                    FlatDetailEvents.FlatLoaded(it)
                }
            }
            else -> {
                onlinerRepository.getFlatById(flatId).asLCE().map {
                    FlatDetailEvents.FlatLoaded(it)
                }
            }
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
            title = "",
            adId = appFlat.adId,
            platform = appFlat.flatPlatform,
            flatUrl = appFlat.flatDetailUrl,
            additionalParams = appFlat.additionalParams?.let {
                UiAdditionalParams(
                    forWhom = it.forWhom,
                    hasWashingMachine = it.hasWashingMachine,
                    hasStove = it.hasStove,
                    hasMicrowave = it.hasMicrowave,
                    hasWifi = it.hasWifi,
                    hasFurniture = it.hasFurniture,
                    hasConditioner = it.hasConditioner
                )
            },
            description = appFlat.description.orEmpty(),
            imageUrls = appFlat.imageUrls.orEmpty(),
            priceUsd = "${appFlat.priceUsd} USD",
            priceByn = "${appFlat.priceByn} BYN",
            address = appFlat.address.orEmpty(),
            district = appFlat.district.orEmpty(),
            numberOfRooms = appFlat.rooms,
            totalArea = appFlat.totalArea?.let { "${it.toInt()}м²" },
            floor = appFlat.floor?.toString(),
            totalFloors = appFlat.totalFloors?.toString(),
            sleepingPlaces = appFlat.sleepingPlaces?.toString(),
            isStudio = appFlat.isStudio,
            bathroomType = appFlat.bathroomType?.let { getBathroomTypeText(it) },
            balconyType = appFlat.balconyType?.let { getBalconyTypeText(it) },
            repairType = appFlat.repairType?.let { getRepairTypeText(it) },
            windowDirection = appFlat.windowDirections?.map { getWindowDirectionText(it) }
                .orEmpty(),
            buildingImprovement = appFlat.buildingImprovements?.map { getBuildingImprovementText(it) }
                .orEmpty(),
            prepaymentType = appFlat.prepaymentType?.let { getPrepaymentTypeText(it) },
            yearBuilt = appFlat.yearBuilt.toString()
        )
    }

    // Вспомогательные функции для преобразования enum в текст
    private fun getBathroomTypeText(type: BathroomType): String = when (type) {
        BathroomType.SEPARATE -> "Раздельный"
        BathroomType.COMBINED -> "Совмещенный"
    }

    private fun getBalconyTypeText(type: BalconyType): String = when (type) {
        BalconyType.LOGGIA -> "Лоджия"
        else -> ""
    }

    private fun getRepairTypeText(type: RepairType): String = when (type) {
        RepairType.COSMETIC -> "Косметический"
        RepairType.EURO -> "Евро"
    }

    private fun getWindowDirectionText(direction: WindowDirection): String = when (direction) {
        WindowDirection.RIVER -> "На речку"
        WindowDirection.PARK -> "В парк"
        WindowDirection.STREET -> "На улицу"
        WindowDirection.SOUTH -> "Юг"
        WindowDirection.WEST -> "Запад"
        else -> {
            ""
        }
    }

    private fun getBuildingImprovementText(improvement: BuildingImprovement): String =
        when (improvement) {
            BuildingImprovement.ELEVATOR -> "Лифт"
            BuildingImprovement.RAMP -> "Пандус"
            BuildingImprovement.GARBAGE_CHUTE -> "Мусоропровод"
            BuildingImprovement.PARKING -> "Парковка"
            BuildingImprovement.INTERCOM -> "Домофон"
            BuildingImprovement.VIDEO_SURVEILLANCE -> "Видеонаблюдение"
        }

    private fun getPrepaymentTypeText(type: PrepaymentType): String = when (type) {
        PrepaymentType.MONTH -> "Месяц"
        PrepaymentType.TWO_MONTHS -> "2 месяца"
        PrepaymentType.DEPOSIT -> "Залог"
    }
}