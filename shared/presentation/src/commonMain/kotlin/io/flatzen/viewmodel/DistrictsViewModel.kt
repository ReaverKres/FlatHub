package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.osm.OsmDistricts
import repository.osm.OsmRepository

sealed interface DistrictsAction : MviAction {
    object LoadDistricts : DistrictsAction
}

sealed interface DistrictsEvent : MviEvent {
    data class DistrictsLoaded(val districts: LCE<List<OsmDistricts?>>) : DistrictsEvent
}

@Immutable
data class DistrictsState(
    val isLoading: Boolean,
    val districts: List<UiDistrict>
) : MviState

@Serializable
data class UiDistrict(
    val id: Long,
    val isChecked: Boolean,
    val nameEn: String,
    val nameLocal: String,
    val coordinates: List<Coordinates>
)

class DistrictsViewModel(
    private val osmRepository: OsmRepository,
    private val filterRepository: FilterRepository
) : BaseMviViewModel<DistrictsAction, DistrictsState, DistrictsEvent, MviEffect>() {
    override fun initialState(): DistrictsState =
        DistrictsState(isLoading = true, districts = emptyList())

    override suspend fun handleIntent(
        action: DistrictsAction,
        currentState: DistrictsState
    ): Flow<DistrictsEvent> {
        return when (action) {
            DistrictsAction.LoadDistricts -> {
                loadDistricts().asLCE().map {
                    DistrictsEvent.DistrictsLoaded(it)
                }
            }
        }
    }

    override suspend fun reduce(
        event: DistrictsEvent,
        currentState: DistrictsState
    ): DistrictsState {
        return when (event) {
            is DistrictsEvent.DistrictsLoaded -> {
                event.districts.process(
                    onLoading = { currentState.copy(isLoading = true) },
                    onError = { _, _ -> currentState.copy(isLoading = false) },
                    onSuccess = { districts ->
                        val uiDistricts = mapToUiDistricts(districts)
                        currentState.copy(
                            isLoading = false,
                            districts = uiDistricts
                        )
                    }
                )
            }
        }
    }

    private fun mapToUiDistricts(districts: List<OsmDistricts?>): List<UiDistrict> {
        return districts.filterNotNull().map { district ->
            UiDistrict(
                id = district.id,
                isChecked = false,
                nameEn = district.nameEn,
                nameLocal = district.nameLocal,
                coordinates = district.coordinates
            )
        }
    }

    private fun loadDistricts(): Flow<List<OsmDistricts?>> {
        val filter = filterRepository.lastFilter()
        val cityName = filter.location?.city?.let {
            LocationUiMapper.findSelectedCity(it)
        }?.displayName ?: ""
        return osmRepository.getCityDistricts(name = cityName)
    }
}