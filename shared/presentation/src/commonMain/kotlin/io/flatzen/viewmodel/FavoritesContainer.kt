package io.flatzen.viewmodel

import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.viewmodel.list.UiFlat
import kotlinx.collections.immutable.toImmutableList
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.mergedrepo.MergedRepository

private typealias FavoritesCtx = PipelineContext<FavoritesState, FavoritesIntent, FavoritesAction>

class FavoritesContainer(
    private val mergedRepository: MergedRepository
) : Container<FavoritesState, FavoritesIntent, FavoritesAction> {

    override val store = store(initial = FavoritesState.Initial) {
        whileSubscribed {
            mergedRepository.getFavoritesFromLocalDb().asLCE().collect { lce ->
                when (lce) {
                    is LCE.Loading -> updateState { copy(isLoading = true) }
                    is LCE.Error -> updateState { copy(isLoading = false) }
                    is LCE.Content -> {
                        val uiFlats = UiFlat.appFlatListToUiFlatList(lce.value)
                        updateState { copy(isLoading = false, flatList = uiFlats) }
                    }
                }
            }
        }
        reduce { intent ->
            when (intent) {
                is FavoritesIntent.ClickOnFavorite -> handleClickOnFavorite(intent)
            }
        }
    }

    private suspend fun FavoritesCtx.handleClickOnFavorite(intent: FavoritesIntent.ClickOnFavorite) {
        mergedRepository.saveFlatToFavorite(intent.flatPlatform, intent.adId).asLCE()
            .collect { lce ->
                when (lce) {
                    is LCE.Loading -> { /* keep state */
                    }

                    is LCE.Error -> { /* keep state */
                    }

                    is LCE.Content -> {
                        val updatedFlat = lce.value
                        updateState {
                            val updatedList = when (updatedFlat) {
                                null -> flatList.filterNot {
                                    it.adId == intent.adId && it.flatPlatform == intent.flatPlatform
                                }

                                else -> flatList.filterNot {
                                    it.adId == updatedFlat.adId && it.flatPlatform == updatedFlat.flatPlatform
                                }
                            }
                            copy(flatList = updatedList.toImmutableList())
                        }
                    }
                }
            }
    }
}
