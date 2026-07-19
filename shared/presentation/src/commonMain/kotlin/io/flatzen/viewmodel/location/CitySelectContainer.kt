package io.flatzen.viewmodel.location

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

@Immutable
data class CitySelectState(
    val query: String = "",
) : MVIState {
    companion object {
        val Initial = CitySelectState()
    }
}

sealed interface CitySelectIntent : MVIIntent {
    data class UpdateQuery(val query: String) : CitySelectIntent
    data object NavigateBack : CitySelectIntent
    data object ClearQuery : CitySelectIntent
}

/**
 * City/country select screen container — owns search query.
 * Country/city selection stays on [io.flatzen.viewmodel.filter.FilterContainer].
 */
class CitySelectContainer(
    private val navigator: FlatHubNavigator,
) : Container<CitySelectState, CitySelectIntent, Nothing> {

    override val store = store(initial = CitySelectState.Initial) {
        reduce { intent ->
            when (intent) {
                is CitySelectIntent.UpdateQuery ->
                    updateState { copy(query = intent.query) }

                CitySelectIntent.ClearQuery ->
                    updateState { copy(query = "") }

                CitySelectIntent.NavigateBack ->
                    navigator.navigate(FlatHubCommand.NavigateBack)
            }
        }
    }
}

/**
 * Build visible country→cities sections for the current query.
 * Match on latin catalog names and UI-localized names.
 *
 * Empty query → selected country's cities only (caller passes [selectedCountry]).
 * Non-empty → sections for every country with matching country or city names.
 */
fun buildCitySelectSections(
    query: String,
    selectedCountry: CountryCode?,
    countryLatin: Map<CountryCode, String>,
    countryLocalized: Map<CountryCode, String>,
    cityLatin: Map<CityCode, String>,
    cityLocalized: Map<CityCode, String>,
): List<Pair<CountryCode, List<CityCode>>> {
    val q = query.trim()
    if (q.isEmpty()) {
        val country = selectedCountry ?: return emptyList()
        return listOf(country to LocationUiMapper.cities(country).map { it.code })
    }

    val sections = mutableListOf<Pair<CountryCode, List<CityCode>>>()
    for (country in LocationUiMapper.countries()) {
        val countryMatches = LocationUiMapper.matchesQuery(
            query = q,
            latinName = countryLatin[country.code].orEmpty(),
            localizedName = countryLocalized[country.code].orEmpty(),
        )
        val matchingCities = LocationUiMapper.cities(country.code).map { it.code }.filter { city ->
            LocationUiMapper.matchesQuery(
                query = q,
                latinName = cityLatin[city].orEmpty(),
                localizedName = cityLocalized[city].orEmpty(),
            )
        }
        when {
            matchingCities.isNotEmpty() -> sections += country.code to matchingCities
            countryMatches ->
                sections += country.code to LocationUiMapper.cities(country.code).map { it.code }
        }
    }
    return sections
}
