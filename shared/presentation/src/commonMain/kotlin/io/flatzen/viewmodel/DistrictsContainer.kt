package io.flatzen.viewmodel

import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.mappers.LocationUiMapper
import kotlinx.coroutines.flow.Flow
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.osm.OsmDistricts
import repository.osm.OsmRepository

private typealias Ctx = PipelineContext<DistrictsState, DistrictsIntent, DistrictsAction>

class DistrictsContainer(
    private val osmRepository: OsmRepository,
    private val filterRepository: FilterRepository
) : Container<DistrictsState, DistrictsIntent, DistrictsAction> {

    override val store = store(initial = DistrictsState.Initial) {
        reduce { intent ->
            when (intent) {
                is DistrictsIntent.LoadDistricts -> loadDistricts()
            }
        }
    }

    private suspend fun Ctx.loadDistricts() {
        updateState { copy(isLoading = true) }
        loadDistrictsFlow().asLCE().collect { lce ->
            when (lce) {
                is LCE.Loading -> updateState { copy(isLoading = true) }
                is LCE.Error -> updateState { copy(isLoading = false) }
                is LCE.Content -> {
                    val uiDistricts = UiDistrict.mapFromModelToUi(lce.value)
                    updateState { copy(isLoading = false, districts = uiDistricts) }
                }
            }
        }
    }

    private fun loadDistrictsFlow(): Flow<List<OsmDistricts?>> {
        val filter = filterRepository.lastFilter()
        val cityName = filter.location?.city?.let {
            LocationUiMapper.findSelectedCity(it)
        }?.displayName ?: "Минск"
        return osmRepository.getCityDistricts(name = cityName)
    }
}
