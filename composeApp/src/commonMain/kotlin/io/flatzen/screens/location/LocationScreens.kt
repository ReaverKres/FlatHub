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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.viewmodel.DistrictsContainer
import io.flatzen.viewmodel.DistrictsIntent
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.MetroLineState
import io.flatzen.viewmodel.filter.UiMetroStation
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
                    val country = state.filters.location?.selectedCountry?.name
                        ?: state.filters.location?.selectedCountry?.code?.name.orEmpty()
                    val city = state.filters.location?.selectedCity?.displayName.orEmpty()
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

            val selectedCityCode = state.filters.location?.selectedCity?.code
            if (
                selectedCityCode == CityCode.MINSK ||
                selectedCityCode == CityCode.WARSZAWA ||
                selectedCityCode == CityCode.TBILISI ||
                selectedCityCode == CityCode.ALMATY
            ) {
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

            Card(modifier = Modifier.fillMaxWidth().clickable {
                filterContainer.intent(FilterScreenAction.OpenDistricts)
            }) {
                Row(modifier = Modifier.padding(16.dp)) {
                    BadgedBox(badge = {
                        val count = 0
                        if (count > 0) Badge { Text(count.toString()) }
                    }) {
                        Text(stringResource(Res.string.location_districts))
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
    filterContainer: FilterContainer = container()
) {
    val state by filterContainer.store.subscribe { }
    val countries = remember { LocationUiMapper.countries() }
    val selectedCountry = state.filters.location?.selectedCountry?.code
    val cities = state.filters.location?.availableCities.orEmpty()
    val selectedCityCode = state.filters.location?.selectedCity?.code
    // Lag content behind selection so the list never paints a new market while still visible.
    var displayCountry by remember { mutableStateOf(selectedCountry) }
    var displayCities by remember { mutableStateOf(cities) }
    var citiesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCountry, cities) {
        if (selectedCountry == displayCountry) {
            displayCities = cities
            if (selectedCountry != null && !citiesVisible) {
                citiesVisible = true
            }
            return@LaunchedEffect
        }
        citiesVisible = false
        delay(210)
        displayCountry = selectedCountry
        displayCities = cities
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
                    IconButton(onClick = { filterContainer.intent(FilterScreenAction.NavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
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
                    Text(
                        text = stringResource(Res.string.location_select_country_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                            countries.forEach { country ->
                                val selected = selectedCountry == country.code
                                CountryMarketCard(
                                    name = country.displayName,
                                    countryCode = country.code,
                                    selected = selected,
                                    modifier = Modifier.width(itemWidth),
                                    onClick = {
                                        if (selectedCountry != country.code) {
                                            citiesVisible = false
                                        }
                                        filterContainer.intent(
                                            FilterScreenAction.SelectCountry(country.code)
                                        )
                                    },
                                )
                            }
                        }
                    }

                    val showBetaNotice = selectedCountry != null &&
                            selectedCountry != CountryCode.BY
                    AnimatedVisibility(
                        visible = showBetaNotice,
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
                    visible = citiesVisible && displayCountry != null,
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

                        displayCities.forEachIndexed { index, city ->
                            val checked = selectedCityCode == city.code
                            CitySelectRow(
                                name = city.displayName,
                                checked = checked,
                                index = index,
                                onClick = {
                                    filterContainer.intent(
                                        FilterScreenAction.SelectCity(city.code)
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
                .height(88.dp)
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
                    modifier = Modifier.fillMaxWidth(),
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
                                .size(14.dp)
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

            val useNumberedMetroLines = when (state.filters.location?.selectedCity?.code) {
                CityCode.WARSZAWA, CityCode.TBILISI -> true
                // Almaty is a single line — keep "blue" title, not M1/M2.
                else -> false
            }
            val lineItems = listOf(
                MetroLineUi(
                    line = MetroLineState.BLUE,
                    title = if (useNumberedMetroLines) {
                        stringResource(Res.string.location_metro_line_m1)
                    } else {
                        stringResource(Res.string.location_metro_line_blue)
                    },
                    color = Color(0xFF1976D2),
                    stations = stationsOf(MetroLineState.BLUE),
                    allStationsOnLine = stations.filter { it.line == MetroLineState.BLUE },
                ),
                MetroLineUi(
                    line = MetroLineState.RED,
                    title = if (useNumberedMetroLines) {
                        stringResource(Res.string.location_metro_line_m2)
                    } else {
                        stringResource(Res.string.location_metro_line_red)
                    },
                    color = Color(0xFFD32F2F),
                    stations = stationsOf(MetroLineState.RED),
                    allStationsOnLine = stations.filter { it.line == MetroLineState.RED },
                ),
                MetroLineUi(
                    line = MetroLineState.GREEN,
                    title = stringResource(Res.string.location_metro_line_green),
                    color = Color(0xFF388E3C),
                    stations = stationsOf(MetroLineState.GREEN),
                    allStationsOnLine = stations.filter { it.line == MetroLineState.GREEN },
                ),
            ).filter { it.stations.isNotEmpty() }

            if (lineItems.isNotEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                ) {
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
                                        enabled = !withAnyMetro,
                                        onToggleLine = { selected ->
                                            filterContainer.intent(
                                                FilterScreenAction.UpdateMetroLine(
                                                    lineUi.line,
                                                    selected
                                                )
                                            )
                                        },
                                        onToggleStation = { station, selected ->
                                            filterContainer.intent(
                                                FilterScreenAction.UpdateMetroFilter(
                                                    station.copy(selected = selected)
                                                )
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
}

private data class MetroLineUi(
    val line: MetroLineState,
    val title: String,
    val color: Color,
    val stations: List<UiMetroStation>,
    val allStationsOnLine: List<UiMetroStation>,
)

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
            items(stations, key = { it.name }) { station ->
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
