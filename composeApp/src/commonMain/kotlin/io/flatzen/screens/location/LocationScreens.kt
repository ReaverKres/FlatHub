package io.flatzen.screens.location

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.add
import flatzen.composeapp.generated.resources.delete
import flatzen.composeapp.generated.resources.location_address_hint
import flatzen.composeapp.generated.resources.location_beta
import flatzen.composeapp.generated.resources.location_city
import flatzen.composeapp.generated.resources.location_country
import flatzen.composeapp.generated.resources.location_country_city
import flatzen.composeapp.generated.resources.location_districts
import flatzen.composeapp.generated.resources.location_market_beta_notice
import flatzen.composeapp.generated.resources.location_metro
import flatzen.composeapp.generated.resources.location_metro_any_switch
import flatzen.composeapp.generated.resources.location_metro_line_blue
import flatzen.composeapp.generated.resources.location_metro_line_green
import flatzen.composeapp.generated.resources.location_metro_line_m1
import flatzen.composeapp.generated.resources.location_metro_line_m2
import flatzen.composeapp.generated.resources.location_metro_line_red
import flatzen.composeapp.generated.resources.location_saved_areas
import flatzen.composeapp.generated.resources.location_search_country_city
import flatzen.composeapp.generated.resources.location_search_district
import flatzen.composeapp.generated.resources.location_search_station
import flatzen.composeapp.generated.resources.location_select_city_hint
import flatzen.composeapp.generated.resources.location_select_country_hint
import flatzen.composeapp.generated.resources.location_title
import flatzen.composeapp.generated.resources.reset
import io.flatzen.animations.rememberShimmerProgress
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.platformsForMarket
import io.flatzen.di.container
import io.flatzen.kmpapp.screens.ShimmerBox
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.viewmodel.DistrictsContainer
import io.flatzen.viewmodel.DistrictsIntent
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.MetroLineState
import io.flatzen.viewmodel.filter.UiMetroStation
import io.flatzen.viewmodel.location.CitySelectContainer
import io.flatzen.viewmodel.location.CitySelectIntent
import io.flatzen.viewmodel.location.buildCitySelectSections
import io.flatzen.widgets.AppSwitch
import io.flatzen.widgets.dialogs.SavedAreasDialog
import io.flatzen.widgets.platformImage
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen() {
    val filterContainer: FilterContainer = container()
    val state by filterContainer.store.subscribe { }

    var addressInput by remember { mutableStateOf("") }
    val addresses = state.filters.address?.toMutableSet() ?: mutableSetOf()

    if (state.savedAreasDialogState.isVisible) {
        SavedAreasDialog(
            state = state.savedAreasDialogState,
            onCheckArea = { id, isChecked ->
                filterContainer.intent(FilterScreenAction.ActivateMapArea(id, isChecked))
            },
            onDeleteArea = {
                filterContainer.intent(FilterScreenAction.DeleteMapArea(it))
            },
            onDismiss = {
                filterContainer.intent(FilterScreenAction.HideSavedAreaListDialog)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(Res.string.location_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { filterContainer.intent(FilterScreenAction.NavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        filterContainer.intent(FilterScreenAction.ClearLocationFilters)
                    }) {
                        Text(stringResource(Res.string.reset))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filterContainer.intent(FilterScreenAction.OpenCity)
                    },
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    val countryCode = state.filters.location?.selectedCountry?.code
                    val cityCode = state.filters.location?.selectedCity?.code
                    val country = countryCode?.localizedName().orEmpty()
                    val city = cityCode?.localizedName().orEmpty()
                    Text(
                        text = listOf(country, city).filter { it.isNotBlank() }.joinToString(" · "),
                    )
                }
            }

            // Поле ввода адреса
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.location_address_hint)) },
                    maxLines = 1,
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (addressInput.isNotBlank()) {
                            filterContainer.intent(FilterScreenAction.AddAddress(addressInput))
                            addressInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add)
                    ) // Можешь заменить на Add
                }
            }

            // Chips список
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                addresses.forEach { addr ->
                    AssistChip(
                        onClick = { /* ничего, просто Chip */ },
                        label = { Text(addr.address) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete, // Можешь заменить на Close
                                contentDescription = stringResource(Res.string.delete),
                                modifier = Modifier.clickable {
                                    filterContainer.intent(
                                        FilterScreenAction.UpdateAddressFilter(
                                            addresses - addr
                                        )
                                    )
                                }
                            )
                        }
                    )
                }
            }

            if (state.hasMetroFilter) {
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    filterContainer.intent(FilterScreenAction.OpenMetro)
                }) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        BadgedBox(badge = {
                            val count = state.filters.metroStationsState.filter { it.selected }.size
                            when {
                                state.filters.withAnyMetro -> Badge()
                                count > 0 -> Badge { Text(count.toString()) }
                            }
                        }) {
                            Text(stringResource(Res.string.location_metro))
                        }
                    }
                }
            }

            if (state.hasDistrictsFilter) {
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    filterContainer.intent(FilterScreenAction.OpenDistricts)
                }) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        BadgedBox(badge = {
                            val count = state.filters.districtsArea?.count { it.isChecked } ?: 0
                            if (count > 0) Badge { Text(count.toString()) }
                        }) {
                            Text(stringResource(Res.string.location_districts))
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().clickable {
                filterContainer.intent(FilterScreenAction.ShowSavedAreaListDialog)
            }) {
                Row(modifier = Modifier.padding(16.dp)) {
                    BadgedBox(badge = {
                        val count = state.filters.userMapAreas?.filter { it.isActive }?.size ?: 0
                        if (count > 0) Badge { Text(count.toString()) }
                    }) {
                        Text(stringResource(Res.string.location_saved_areas))
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectScreen(
    filterContainer: FilterContainer = container(),
    citySelectContainer: CitySelectContainer = container(),
) {
    val filterState by filterContainer.store.subscribe { }
    val citySelectState by citySelectContainer.store.subscribe { }
    val selectedCountry = filterState.filters.location?.selectedCountry?.code
    val selectedCityCode = filterState.filters.location?.selectedCity?.code
    val query = citySelectState.query
    val searching = query.isNotBlank()

    val countryLatin = remember {
        LocationUiMapper.countries().associate { it.code to it.latinName }
    }
    val cityLatin = remember {
        LocationUiMapper.allCities().associate { it.code to it.latinName }
    }
    val countryLocalized = CountryCode.entries.associateWith { it.localizedName() }
    val cityLocalized = CityCode.entries.associateWith { it.localizedName() }

    val visibleCountries =
        remember(query, countryLatin, countryLocalized, cityLatin, cityLocalized) {
            if (!searching) {
                LocationUiMapper.countries().map { it.code }
            } else {
                buildCitySelectSections(
                    query = query,
                    selectedCountry = selectedCountry,
                    countryLatin = countryLatin,
                    countryLocalized = countryLocalized,
                    cityLatin = cityLatin,
                    cityLocalized = cityLocalized,
                ).map { it.first }
            }
        }

    val citySections = remember(
        query,
        selectedCountry,
        countryLatin,
        countryLocalized,
        cityLatin,
        cityLocalized,
    ) {
        buildCitySelectSections(
            query = query,
            selectedCountry = selectedCountry,
            countryLatin = countryLatin,
            countryLocalized = countryLocalized,
            cityLatin = cityLatin,
            cityLocalized = cityLocalized,
        )
    }

    // Lag city list behind selection so the list never paints a new market while still visible.
    var displaySections by remember { mutableStateOf(citySections) }
    var citiesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCountry, citySections, searching) {
        if (searching) {
            displaySections = citySections
            citiesVisible = citySections.isNotEmpty()
            return@LaunchedEffect
        }
        if (selectedCountry == displaySections.firstOrNull()?.first &&
            displaySections.map { it.second } == citySections.map { it.second }
        ) {
            displaySections = citySections
            if (selectedCountry != null && !citiesVisible) {
                citiesVisible = true
            }
            return@LaunchedEffect
        }
        citiesVisible = false
        delay(210)
        displaySections = citySections
        if (selectedCountry != null) {
            delay(16)
            citiesVisible = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(Res.string.location_country_city),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            citySelectContainer.intent(CitySelectIntent.NavigateBack)
                        },
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        citySelectContainer.intent(CitySelectIntent.UpdateQuery(it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                    placeholder = {
                        Text(stringResource(Res.string.location_search_country_city))
                    },
                    singleLine = true,
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.location_country),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (!searching) {
                        Text(
                            text = stringResource(Res.string.location_select_country_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val gap = 10.dp
                        val maxPerRow = 3
                        val itemWidth = (maxWidth - gap * (maxPerRow - 1)) / maxPerRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap),
                            maxItemsInEachRow = maxPerRow,
                        ) {
                            visibleCountries.forEach { countryCode ->
                                val selected = selectedCountry == countryCode
                                CountryMarketCard(
                                    name = countryCode.localizedName(),
                                    countryCode = countryCode,
                                    selected = selected,
                                    modifier = Modifier.width(itemWidth),
                                    onClick = {
                                        if (!searching && selectedCountry != countryCode) {
                                            citiesVisible = false
                                        }
                                        citySelectContainer.intent(CitySelectIntent.ClearQuery)
                                        filterContainer.intent(
                                            FilterScreenAction.SelectCountry(countryCode),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    val showBetaNotice = selectedCountry != null &&
                            selectedCountry != CountryCode.BY
                    AnimatedVisibility(
                        visible = showBetaNotice && !searching,
                        enter = fadeIn(tween(280)) + expandVertically(tween(320)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(180)),
                    ) {
                        MarketBetaNotice()
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item(key = "cities_block") {
                AnimatedVisibility(
                    visible = citiesVisible && displaySections.isNotEmpty(),
                    enter = fadeIn(
                        animationSpec = tween(360, easing = FastOutSlowInEasing),
                    ) + expandVertically(
                        animationSpec = tween(420, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top,
                    ) + slideInVertically(
                        animationSpec = tween(420, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullHeight -> fullHeight / 14 },
                    ),
                    exit = fadeOut(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    ) + shrinkVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top,
                    ) + slideOutVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        targetOffsetY = { fullHeight -> -fullHeight / 20 },
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var rowIndex = 0
                        displaySections.forEach { (countryCode, cityCodes) ->
                            if (searching || displaySections.size > 1) {
                                Text(
                                    text = countryCode.localizedName(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 8.dp, bottom = 4.dp),
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 4.dp, bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.location_city),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                    Text(
                                        text = stringResource(Res.string.location_select_city_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            cityCodes.forEach { cityCode ->
                                val checked = selectedCityCode == cityCode
                                val index = rowIndex++
                                CitySelectRow(
                                    name = cityCode.localizedName(),
                                    checked = checked,
                                    index = index,
                                    onClick = {
                                        filterContainer.intent(
                                            FilterScreenAction.SelectCity(cityCode),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.CitySelectRow(
    name: String,
    checked: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val stagger = (36 * index).coerceAtMost(280)
    Column(
        modifier = Modifier.animateEnterExit(
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 320,
                    delayMillis = 60 + stagger,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 340,
                    delayMillis = 60 + stagger,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { it / 5 },
            ),
            exit = fadeOut(tween(120, easing = FastOutSlowInEasing)),
        ),
    ) {
        ListItem(
            headlineContent = { Text(name) },
            trailingContent = {
                if (checked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = ListItemDefaults.colors(
                containerColor = if (checked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                },
            ),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MarketBetaNotice() {
    val uriHandler = LocalUriHandler.current
    val notice = stringResource(Res.string.location_market_beta_notice)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { uriHandler.openUri("https://t.me/FlatHub_appbot") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFE0B2).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.55f)),
    ) {
        Text(
            text = notice,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun CountryMarketCard(
    name: String,
    countryCode: CountryCode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val platforms = remember(countryCode) { platformsForMarket(countryCode) }
    val showBeta = countryCode != CountryCode.BY
    val cardColors = if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = cardColors,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    platforms.forEach { platform ->
                        Image(
                            painter = platform.platformImage(),
                            contentDescription = platform.value,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            alpha = if (selected) 1f else 0.85f,
                        )
                    }
                }
            }
        }
        if (showBeta) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF57C00),
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = stringResource(Res.string.location_beta),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroSelectScreen(
    filterContainer: FilterContainer = container()
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val state by filterContainer.store.subscribe { }
    val withAnyMetro = state.filters.withAnyMetro
    val stations = state.filters.metroStationsState
    val queryText = query.text.lowercase()

    fun stationsOf(line: MetroLineState): List<UiMetroStation> =
        stations.filter {
            it.line == line && it.name.lowercase().contains(queryText)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(Res.string.location_metro),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        filterContainer.intent(FilterScreenAction.NavigateBack)
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.location_search_station)) },
                enabled = !withAnyMetro,
            )

            AppSwitch(
                label = stringResource(Res.string.location_metro_any_switch),
                state = withAnyMetro,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                filterContainer.intent(FilterScreenAction.UpdateWithAnyMetro(it))
            }

            val cityCode = state.filters.location?.selectedCity?.code
            val useNumberedMetroLines = when (cityCode) {
                CityCode.WARSZAWA, CityCode.TBILISI -> true
                else -> false
            }
            val lineItems = MetroStationsMapper.lineOrderForCity(cityCode).mapNotNull { line ->
                val onLine = stations.filter { it.line == line }
                if (onLine.isEmpty()) return@mapNotNull null
                MetroLineUi(
                    line = line,
                    title = metroLineTitle(line, useNumberedMetroLines),
                    color = metroLineColor(line),
                    stations = stationsOf(line),
                    allStationsOnLine = onLine,
                )
            }

            if (lineItems.isNotEmpty()) {
                val onToggleLine: (MetroLineState, Boolean) -> Unit = { line, selected ->
                    filterContainer.intent(FilterScreenAction.UpdateMetroLine(line, selected))
                }
                val onToggleStation: (UiMetroStation, Boolean) -> Unit = { station, selected ->
                    filterContainer.intent(
                        FilterScreenAction.UpdateMetroFilter(station.copy(selected = selected))
                    )
                }

                if (lineItems.size <= MetroGridMaxLines) {
                    MetroLinesGrid(
                        lineItems = lineItems,
                        enabled = !withAnyMetro,
                        onToggleLine = onToggleLine,
                        onToggleStation = onToggleStation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                    )
                } else {
                    MetroLinesMasterDetail(
                        lineItems = lineItems,
                        cityCode = cityCode,
                        queryText = queryText,
                        enabled = !withAnyMetro,
                        onToggleLine = onToggleLine,
                        onToggleStation = onToggleStation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** Keep the multi-column grid only for small networks (e.g. Minsk, Warsaw). */
private const val MetroGridMaxLines = 3

@Composable
private fun MetroLinesGrid(
    lineItems: List<MetroLineUi>,
    enabled: Boolean,
    onToggleLine: (MetroLineState, Boolean) -> Unit,
    onToggleStation: (UiMetroStation, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val gap = 8.dp
        val minColumnWidth = 140.dp
        val columnsCount = max(
            1,
            ((maxWidth + gap) / (minColumnWidth + gap)).toInt()
        ).coerceAtMost(lineItems.size)
        val rows = lineItems.chunked(columnsCount)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    rowItems.forEach { lineUi ->
                        MetroLineColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            title = lineUi.title,
                            lineColor = lineUi.color,
                            stations = lineUi.stations,
                            allStationsOnLine = lineUi.allStationsOnLine,
                            enabled = enabled,
                            onToggleLine = { selected -> onToggleLine(lineUi.line, selected) },
                            onToggleStation = onToggleStation,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetroLinesMasterDetail(
    lineItems: List<MetroLineUi>,
    cityCode: CityCode?,
    queryText: String,
    enabled: Boolean,
    onToggleLine: (MetroLineState, Boolean) -> Unit,
    onToggleStation: (UiMetroStation, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLine by remember(cityCode) { mutableStateOf<MetroLineState?>(null) }
    val chipsState = rememberLazyListState()

    val stillValid = selectedLine != null && lineItems.any { it.line == selectedLine }
    val resolvedLine = when {
        stillValid && queryText.isBlank() -> selectedLine
        stillValid && lineItems.first { it.line == selectedLine }.stations.isNotEmpty() ->
            selectedLine

        queryText.isNotBlank() ->
            lineItems.firstOrNull { it.stations.isNotEmpty() }?.line
                ?: selectedLine?.takeIf { stillValid }
                ?: lineItems.firstOrNull()?.line

        else ->
            lineItems.firstOrNull { line -> line.allStationsOnLine.any { it.selected } }?.line
                ?: selectedLine?.takeIf { stillValid }
                ?: lineItems.firstOrNull()?.line
    }

    LaunchedEffect(resolvedLine) {
        if (resolvedLine != null && selectedLine != resolvedLine) {
            selectedLine = resolvedLine
        }
        val index = lineItems.indexOfFirst { it.line == resolvedLine }
        if (index >= 0) {
            chipsState.animateScrollToItem(index)
        }
    }

    val activeLine = lineItems.firstOrNull { it.line == resolvedLine } ?: lineItems.first()

    Column(modifier = modifier) {
        LazyRow(
            state = chipsState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(lineItems, key = { it.line }) { lineUi ->
                val selectedCount = lineUi.allStationsOnLine.count { it.selected }
                val isSelected = lineUi.line == resolvedLine
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLine = lineUi.line },
                    enabled = enabled,
                    label = {
                        Text(
                            text = if (selectedCount > 0) {
                                "${lineUi.title} · $selectedCount"
                            } else {
                                lineUi.title
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Surface(
                            color = lineUi.color,
                            shape = CircleShape,
                            modifier = Modifier.size(10.dp),
                        ) {}
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = lineUi.color.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = lineUi.color,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = enabled,
                        selected = isSelected,
                        selectedBorderColor = lineUi.color,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        MetroLineColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            title = activeLine.title,
            lineColor = activeLine.color,
            stations = activeLine.stations,
            allStationsOnLine = activeLine.allStationsOnLine,
            enabled = enabled,
            onToggleLine = { selected -> onToggleLine(activeLine.line, selected) },
            onToggleStation = onToggleStation,
        )
    }
}

private data class MetroLineUi(
    val line: MetroLineState,
    val title: String,
    val color: Color,
    val stations: List<UiMetroStation>,
    val allStationsOnLine: List<UiMetroStation>,
)

@Composable
private fun metroLineTitle(line: MetroLineState, useNumbered: Boolean): String = when (line) {
    MetroLineState.BLUE -> if (useNumbered) {
        stringResource(Res.string.location_metro_line_m1)
    } else {
        stringResource(Res.string.location_metro_line_blue)
    }

    MetroLineState.RED -> if (useNumbered) {
        stringResource(Res.string.location_metro_line_m2)
    } else {
        stringResource(Res.string.location_metro_line_red)
    }

    MetroLineState.GREEN -> stringResource(Res.string.location_metro_line_green)

    MetroLineState.BAKERLOO -> "Bakerloo"
    MetroLineState.CENTRAL -> "Central"
    MetroLineState.CIRCLE -> "Circle"
    MetroLineState.DISTRICT -> "District"
    MetroLineState.HAMMERSMITH_CITY -> "Hammersmith & City"
    MetroLineState.JUBILEE -> "Jubilee"
    MetroLineState.METROPOLITAN -> "Metropolitan"
    MetroLineState.NORTHERN -> "Northern"
    MetroLineState.PICCADILLY -> "Piccadilly"
    MetroLineState.VICTORIA -> "Victoria"
    MetroLineState.WATERLOO_CITY -> "Waterloo & City"
    MetroLineState.ELIZABETH -> "Elizabeth"

    MetroLineState.PARIS_M1 -> "M1"
    MetroLineState.PARIS_M2 -> "M2"
    MetroLineState.PARIS_M3 -> "M3"
    MetroLineState.PARIS_M4 -> "M4"
    MetroLineState.PARIS_M5 -> "M5"
    MetroLineState.PARIS_M6 -> "M6"
    MetroLineState.PARIS_M7 -> "M7"
    MetroLineState.PARIS_M8 -> "M8"
    MetroLineState.PARIS_M9 -> "M9"
    MetroLineState.PARIS_M10 -> "M10"
    MetroLineState.PARIS_M11 -> "M11"
    MetroLineState.PARIS_M12 -> "M12"
    MetroLineState.PARIS_M13 -> "M13"
    MetroLineState.PARIS_M14 -> "M14"
    MetroLineState.PARIS_RER_A -> "RER A"
    MetroLineState.PARIS_RER_B -> "RER B"

    MetroLineState.WIEN_U1 -> "U1"
    MetroLineState.WIEN_U2 -> "U2"
    MetroLineState.WIEN_U3 -> "U3"
    MetroLineState.WIEN_U4 -> "U4"
    MetroLineState.WIEN_U6 -> "U6"

    MetroLineState.SEOUL_1 -> "Line 1"
    MetroLineState.SEOUL_2 -> "Line 2"
    MetroLineState.SEOUL_3 -> "Line 3"
    MetroLineState.SEOUL_4 -> "Line 4"
    MetroLineState.SEOUL_5 -> "Line 5"
    MetroLineState.SEOUL_6 -> "Line 6"
    MetroLineState.SEOUL_7 -> "Line 7"
    MetroLineState.SEOUL_8 -> "Line 8"
    MetroLineState.SEOUL_9 -> "Line 9"

    MetroLineState.BERLIN_U1 -> "U1"
    MetroLineState.BERLIN_U2 -> "U2"
    MetroLineState.BERLIN_U3 -> "U3"
    MetroLineState.BERLIN_U4 -> "U4"
    MetroLineState.BERLIN_U5 -> "U5"
    MetroLineState.BERLIN_U6 -> "U6"
    MetroLineState.BERLIN_U7 -> "U7"
    MetroLineState.BERLIN_U8 -> "U8"
    MetroLineState.BERLIN_U9 -> "U9"
    MetroLineState.BERLIN_S1 -> "S1"
    MetroLineState.BERLIN_S2 -> "S2"
    MetroLineState.BERLIN_S3 -> "S3"
    MetroLineState.BERLIN_S5 -> "S5"
    MetroLineState.BERLIN_S7 -> "S7"
    MetroLineState.BERLIN_S9 -> "S9"

    MetroLineState.MADRID_1 -> "L1"
    MetroLineState.MADRID_2 -> "L2"
    MetroLineState.MADRID_3 -> "L3"
    MetroLineState.MADRID_4 -> "L4"
    MetroLineState.MADRID_5 -> "L5"
    MetroLineState.MADRID_6 -> "L6"
    MetroLineState.MADRID_7 -> "L7"
    MetroLineState.MADRID_8 -> "L8"
    MetroLineState.MADRID_9 -> "L9"
    MetroLineState.MADRID_10 -> "L10"
    MetroLineState.MADRID_11 -> "L11"
    MetroLineState.MADRID_12 -> "L12"
    MetroLineState.MADRID_R -> "Ramal"

    MetroLineState.BCN_L1 -> "L1"
    MetroLineState.BCN_L2 -> "L2"
    MetroLineState.BCN_L3 -> "L3"
    MetroLineState.BCN_L4 -> "L4"
    MetroLineState.BCN_L5 -> "L5"
    MetroLineState.BCN_L9N -> "L9 Nord"
    MetroLineState.BCN_L9S -> "L9 Sud"
    MetroLineState.BCN_L10 -> "L10"
    MetroLineState.BCN_L11 -> "L11"

    MetroLineState.BKK_BTS_SUKHUMVIT -> "BTS Sukhumvit"
    MetroLineState.BKK_BTS_SILOM -> "BTS Silom"
    MetroLineState.BKK_MRT_BLUE -> "MRT Blue"
    MetroLineState.BKK_MRT_PURPLE -> "MRT Purple"
    MetroLineState.BKK_ARL -> "Airport Rail"

    MetroLineState.TOKYO_YAMANOTE -> "JR Yamanote"
    MetroLineState.TOKYO_GINZA -> "Ginza"
    MetroLineState.TOKYO_MARUNOUCHI -> "Marunouchi"
    MetroLineState.TOKYO_HIBIYA -> "Hibiya"
    MetroLineState.TOKYO_TOZAI -> "Tozai"
    MetroLineState.TOKYO_CHIYODA -> "Chiyoda"
    MetroLineState.TOKYO_YURAKUCHO -> "Yurakucho"
    MetroLineState.TOKYO_HANZOMON -> "Hanzomon"
    MetroLineState.TOKYO_NAMBOKU -> "Namboku"
    MetroLineState.TOKYO_FUKUTOSHIN -> "Fukutoshin"
    MetroLineState.TOKYO_ASAKUSA -> "Asakusa"
    MetroLineState.TOKYO_MITA -> "Mita"
    MetroLineState.TOKYO_SHINJUKU -> "Shinjuku"
    MetroLineState.TOKYO_OEDO -> "Oedo"
}

private fun metroLineColor(line: MetroLineState): Color = when (line) {
    MetroLineState.BLUE -> Color(0xFF1976D2)
    MetroLineState.RED -> Color(0xFFD32F2F)
    MetroLineState.GREEN -> Color(0xFF388E3C)

    MetroLineState.BAKERLOO -> Color(0xFFB36305)
    MetroLineState.CENTRAL -> Color(0xFFE32017)
    MetroLineState.CIRCLE -> Color(0xFFFFD300)
    MetroLineState.DISTRICT -> Color(0xFF00782A)
    MetroLineState.HAMMERSMITH_CITY -> Color(0xFFF3A9BB)
    MetroLineState.JUBILEE -> Color(0xFFA0A5A9)
    MetroLineState.METROPOLITAN -> Color(0xFF9B0056)
    MetroLineState.NORTHERN -> Color(0xFF000000)
    MetroLineState.PICCADILLY -> Color(0xFF003688)
    MetroLineState.VICTORIA -> Color(0xFF0098D4)
    MetroLineState.WATERLOO_CITY -> Color(0xFF95CDBA)
    MetroLineState.ELIZABETH -> Color(0xFF6950A1)

    MetroLineState.PARIS_M1 -> Color(0xFFFFCD00)
    MetroLineState.PARIS_M2 -> Color(0xFF003CA6)
    MetroLineState.PARIS_M3 -> Color(0xFF837902)
    MetroLineState.PARIS_M4 -> Color(0xFFCF009E)
    MetroLineState.PARIS_M5 -> Color(0xFFFF7E2E)
    MetroLineState.PARIS_M6 -> Color(0xFF6ECA97)
    MetroLineState.PARIS_M7 -> Color(0xFFFA9ABA)
    MetroLineState.PARIS_M8 -> Color(0xFFE19BDF)
    MetroLineState.PARIS_M9 -> Color(0xFFB6BD00)
    MetroLineState.PARIS_M10 -> Color(0xFFC9910D)
    MetroLineState.PARIS_M11 -> Color(0xFF704B1C)
    MetroLineState.PARIS_M12 -> Color(0xFF007852)
    MetroLineState.PARIS_M13 -> Color(0xFF6EC4E8)
    MetroLineState.PARIS_M14 -> Color(0xFF62259D)
    MetroLineState.PARIS_RER_A -> Color(0xFFF7403A)
    MetroLineState.PARIS_RER_B -> Color(0xFF5291CE)

    MetroLineState.WIEN_U1 -> Color(0xFFE20613)
    MetroLineState.WIEN_U2 -> Color(0xFFA862A4)
    MetroLineState.WIEN_U3 -> Color(0xFFE8702A)
    MetroLineState.WIEN_U4 -> Color(0xFF00963F)
    MetroLineState.WIEN_U6 -> Color(0xFF9D6910)

    MetroLineState.SEOUL_1 -> Color(0xFF0052A4)
    MetroLineState.SEOUL_2 -> Color(0xFF00A84D)
    MetroLineState.SEOUL_3 -> Color(0xFFEF7C1C)
    MetroLineState.SEOUL_4 -> Color(0xFF00A5DE)
    MetroLineState.SEOUL_5 -> Color(0xFF996CAC)
    MetroLineState.SEOUL_6 -> Color(0xFFCD7C2F)
    MetroLineState.SEOUL_7 -> Color(0xFF747F00)
    MetroLineState.SEOUL_8 -> Color(0xFFE6186C)
    MetroLineState.SEOUL_9 -> Color(0xFFBDB092)

    MetroLineState.BERLIN_U1 -> Color(0xFF7DAD4C)
    MetroLineState.BERLIN_U2 -> Color(0xFFDA421E)
    MetroLineState.BERLIN_U3 -> Color(0xFF16683D)
    MetroLineState.BERLIN_U4 -> Color(0xFFF0D722)
    MetroLineState.BERLIN_U5 -> Color(0xFF7E5330)
    MetroLineState.BERLIN_U6 -> Color(0xFF8C6DAB)
    MetroLineState.BERLIN_U7 -> Color(0xFF528DCA)
    MetroLineState.BERLIN_U8 -> Color(0xFF224F86)
    MetroLineState.BERLIN_U9 -> Color(0xFFF3791D)
    MetroLineState.BERLIN_S1 -> Color(0xFFDE4DA4)
    MetroLineState.BERLIN_S2 -> Color(0xFF007734)
    MetroLineState.BERLIN_S3 -> Color(0xFF0066A6)
    MetroLineState.BERLIN_S5 -> Color(0xFFF29EC3)
    MetroLineState.BERLIN_S7 -> Color(0xFF816DA6)
    MetroLineState.BERLIN_S9 -> Color(0xFF992746)

    MetroLineState.MADRID_1 -> Color(0xFF29B0E0)
    MetroLineState.MADRID_2 -> Color(0xFFC4002D)
    MetroLineState.MADRID_3 -> Color(0xFFFFD100)
    MetroLineState.MADRID_4 -> Color(0xFFA05A2C)
    MetroLineState.MADRID_5 -> Color(0xFF8EC63F)
    MetroLineState.MADRID_6 -> Color(0xFF999999)
    MetroLineState.MADRID_7 -> Color(0xFFF07A25)
    MetroLineState.MADRID_8 -> Color(0xFFEB82BD)
    MetroLineState.MADRID_9 -> Color(0xFFA61C4D)
    MetroLineState.MADRID_10 -> Color(0xFF005CA9)
    MetroLineState.MADRID_11 -> Color(0xFF009640)
    MetroLineState.MADRID_12 -> Color(0xFFA49700)
    MetroLineState.MADRID_R -> Color(0xFFCCCCCC)

    MetroLineState.BCN_L1 -> Color(0xFFCE1126)
    MetroLineState.BCN_L2 -> Color(0xFF7B2D8E)
    MetroLineState.BCN_L3 -> Color(0xFF00A650)
    MetroLineState.BCN_L4 -> Color(0xFFFAB914)
    MetroLineState.BCN_L5 -> Color(0xFF0077C8)
    MetroLineState.BCN_L9N -> Color(0xFFFF6B00)
    MetroLineState.BCN_L9S -> Color(0xFFFF8C00)
    MetroLineState.BCN_L10 -> Color(0xFF00A0E3)
    MetroLineState.BCN_L11 -> Color(0xFF8DC63F)

    MetroLineState.BKK_BTS_SUKHUMVIT -> Color(0xFF5BBF21)
    MetroLineState.BKK_BTS_SILOM -> Color(0xFF008C45)
    MetroLineState.BKK_MRT_BLUE -> Color(0xFF1E3A8A)
    MetroLineState.BKK_MRT_PURPLE -> Color(0xFF6B21A8)
    MetroLineState.BKK_ARL -> Color(0xFFC8102E)

    MetroLineState.TOKYO_YAMANOTE -> Color(0xFF9ACD32)
    MetroLineState.TOKYO_GINZA -> Color(0xFFFF9500)
    MetroLineState.TOKYO_MARUNOUCHI -> Color(0xFFE60012)
    MetroLineState.TOKYO_HIBIYA -> Color(0xFFB5B5AC)
    MetroLineState.TOKYO_TOZAI -> Color(0xFF009BBF)
    MetroLineState.TOKYO_CHIYODA -> Color(0xFF00BB85)
    MetroLineState.TOKYO_YURAKUCHO -> Color(0xFFC1A470)
    MetroLineState.TOKYO_HANZOMON -> Color(0xFF8F76D6)
    MetroLineState.TOKYO_NAMBOKU -> Color(0xFF00ADA9)
    MetroLineState.TOKYO_FUKUTOSHIN -> Color(0xFF9C5E31)
    MetroLineState.TOKYO_ASAKUSA -> Color(0xFFE85298)
    MetroLineState.TOKYO_MITA -> Color(0xFF0079C2)
    MetroLineState.TOKYO_SHINJUKU -> Color(0xFF6CBB5A)
    MetroLineState.TOKYO_OEDO -> Color(0xFFB6007A)
}

@Composable
private fun MetroLineColumn(
    modifier: Modifier,
    title: String,
    lineColor: Color,
    stations: List<UiMetroStation>,
    allStationsOnLine: List<UiMetroStation>,
    enabled: Boolean,
    onToggleLine: (Boolean) -> Unit,
    onToggleStation: (UiMetroStation, Boolean) -> Unit,
) {
    val allSelected = allStationsOnLine.isNotEmpty() && allStationsOnLine.all { it.selected }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onToggleLine(!allSelected) }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { onToggleLine(it) },
                enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = lineColor)
            )
            Surface(
                color = lineColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
            ) {}
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = lineColor.copy(alpha = 0.4f))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(stations, key = { "${it.line}_${it.name}" }) { station ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            onToggleStation(station, !station.selected)
                        }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                ) {
                    Checkbox(
                        checked = station.selected,
                        onCheckedChange = { onToggleStation(station, it) },
                        enabled = enabled,
                    )
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictSelectScreen() {
    var query by remember { mutableStateOf(TextFieldValue("")) }

    val filterContainer: FilterContainer = container()
    val filterState by filterContainer.store.subscribe { }


    val districtsContainer: DistrictsContainer = container()
    val districtsState by districtsContainer.store.subscribe { }

    val selectedCityCode = filterState.filters.location?.selectedCity?.code
    val districts = if (filterState.filters.districtsArea.isNullOrEmpty()) {
        districtsState.districts
    } else {
        filterState.filters.districtsArea
    } ?: emptyList()

    val filteredDistricts = districts.filter {
        it.nameLocal.lowercase().contains(query.text.lowercase(), true)
    }

    val shimmerProgress by rememberShimmerProgress()

    LaunchedEffectOnce(selectedCityCode) {
        districtsContainer.store.intent(DistrictsIntent.LoadDistricts)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(Res.string.location_districts), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { filterContainer.intent(FilterScreenAction.NavigateBack) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.location_search_district)) }
            )

            if (districtsState.isLoading) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    repeat(6) {
                        ShimmerBox(
                            modifier = Modifier
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                )
                                .fillMaxWidth()
                                .height(30.dp),
                            shimmerProgress = shimmerProgress
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(filteredDistricts) { district ->
                        ListItem(
                            headlineContent = {
                                Text(district.nameLocal)
                            },
                            trailingContent = {
                                Row {
                                    Checkbox(
                                        checked = district.isChecked,
                                        onCheckedChange = {
                                            filterContainer.intent(
                                                FilterScreenAction.UpdateDistrictFilter(
                                                    districtsState.districts,
                                                    district.copy(isChecked = it)
                                                )
                                            )
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filterContainer.intent(
                                        FilterScreenAction.UpdateDistrictFilter(
                                            districtsState.districts,
                                            district.copy(isChecked = district.isChecked.not())
                                        )
                                    )
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
