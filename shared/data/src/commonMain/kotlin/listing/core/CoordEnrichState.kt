package listing.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared loading flag for background coordinate enrichment (map pins).
 * Cleared when the current enrich batch finishes (all targets marked [entities.FlatDevInfo.coordsEnriched]).
 */
class CoordEnrichState {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    internal fun setLoading(value: Boolean) {
        _isLoading.value = value
    }
}
