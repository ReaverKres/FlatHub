import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.screens.map.RoomMarker
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.viewmodel.ContactInformationUi
import io.flatzen.viewmodel.FlatDetailScreenAction
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.UiDetailFlat
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.OpenInMapButton
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.snapScrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    flatPlatform: FlatPlatform,
    objectId: Long,
    navigateBack: () -> Unit,
    navigateToMap: (flatId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<FlatDetailViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(objectId) {
        viewModel.onIntent(FlatDetailScreenAction.LoadFlatDetails(flatPlatform, objectId))
        // Track screen view through MviAction
        viewModel.onIntent(
            FlatDetailScreenAction.TrackScreenView(
                screenName = AppMetrcica.Screens.DETAIL,
                parameters = mapOf(
                    AppMetrcica.Parameters.FLAT_PLATFORM to flatPlatform.name,
                    AppMetrcica.Parameters.OBJECT_ID to objectId
                )
            )
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {  },
            navigationIcon = {
                IconButton(onClick = navigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        when {
            state.isLoading -> {

            }

            state.error != null -> {
                // ErrorContent(
                //     error = state.error,
                //     onRetry = {
                //         viewModel.onIntent(FlatDetailScreenAction.LoadFlatDetails(flatPlatform, objectId))
                //     },
                //     modifier = Modifier.fillMaxSize()
                // )
            }

            state.flat != null -> {
                FlatDetailContent(
                    flat = state.flat!!,
                    mapState = viewModel.mapState,
                    modifier = Modifier.fillMaxSize(),
                    clickOnFavorite = {
                        viewModel.onIntent(
                            FlatDetailScreenAction.ClickOnFavorite(
                                state.flat!!.platform,
                                state.flat!!.adId
                            )
                        )
                    },
                    navigateToMap = {
                        navigateToMap(state.flat!!.adId)
                    }
                )
            }

            else -> {
                EmptyScreenContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun FlatDetailContent(
    flat: UiDetailFlat,
    mapState: MapState,
    modifier: Modifier = Modifier,
    clickOnFavorite: () -> Unit,
    navigateToMap: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Изображения
        FlatImagePager(
            modifier = Modifier.height(300.dp),
            flatPlatform = flat.platform,
            imageUrls = flat.imageUrls,
            contentScale = ContentScale.Fit,
            savedInFavorite = flat.savedInFavorite,
            clickOnFavorite = clickOnFavorite
        )

        // Основная информация
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о публикации
            if (flat.publishedAt != null || flat.isOwner != null) {
                PublicationInfo(
                    timeAgo = flat.publishedAt,
                    isOwner = flat.isOwner
                )
            }

            SourceLinkSection(flat.platform, flat.flatUrl)

            if (flat.isDetailDataLoaded == true) {
                if (hasContactData(flat.contactInformation)) {
                    ContactInfoSection(contactInfo = flat.contactInformation)
                } else {
                    Text(
                        text = "Для связи перейдите на сайт объявления",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Цены
            PriceSection(
                priceUsd = flat.priceUsd,
                priceByn = flat.priceByn,
                priceUsdSquare = flat.priceUsdSquare,
                priceBynSquare = flat.priceBynSquare
            )

            // Адрес и метро
            AddressSection(
                address = flat.address,
                metroStation = flat.metroStation
            )

            // Краткая информация
            QuickInfoSection(
                rooms = flat.numberOfRooms,
                year = flat.yearBuilt,
                area = flat.totalArea,
                condition = flat.condition
            )

            if(flat.coordinates != null) {
                val coroutineScope = rememberCoroutineScope()
                mapState.apply {
                    val mercatorCoordinates =
                        flat.coordinates?.let { lonLatToNormalized(it.latitude, it.longitude) } ?: return
                    coroutineScope.launch {
                        snapScrollTo(mercatorCoordinates.first, mercatorCoordinates.second)
                    }
                    addMarker(
                        id = flat.adId.toString(),
                        x = mercatorCoordinates.first,
                        y = mercatorCoordinates.second,
                    ) {
                        RoomMarker(
                            flat.numberOfRooms,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp).fillMaxWidth().height(200.dp).clickable {
                    navigateToMap()
                }) {
                    MapUI(modifier = Modifier.fillMaxSize(), state = mapState)
                }

                OpenInMapButton(
                    coordinates = flat.coordinates!!,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            // О квартире
            if (hasApartmentData(flat)) {
                HorizontalDivider()
                AboutApartmentSection(flat = flat)
            }

            // Удобства и оборудование
            if (flat.amenities.isNotEmpty() || flat.kitchenEquipment.isNotEmpty()) {
                HorizontalDivider()
                AmenitiesSection(
                    amenities = flat.amenities,
                    kitchenEquipment = flat.kitchenEquipment
                )
            }

            // Условия проживания
            if (!flat.forWhom.isNullOrEmpty() || !flat.prepaymentType.isNullOrBlank()) {
                HorizontalDivider()
                ConditionsSection(
                    forWhom = flat.forWhom,
                    prepaymentType = flat.prepaymentType
                )
            }

            // О доме
            if (hasBuildingData(flat)) {
                HorizontalDivider()
                AboutBuildingSection(flat = flat)
            }

            // Местоположение
            if (!flat.district.isNullOrBlank() || !flat.metroStation.isNullOrBlank()) {
                HorizontalDivider()
                LocationSection(
                    district = flat.district,
                    address = flat.address,
                    metroStation = flat.metroStation
                )
            }

            // Описание
            if (flat.description.isNotBlank()) {
                HorizontalDivider()
                DescriptionSection(description = flat.description)
            }
        }
    }
}

// Вспомогательные функции для проверки наличия данных
private fun hasApartmentData(flat: UiDetailFlat): Boolean {
    return flat.totalArea != null ||
            flat.livingArea != null ||
            flat.kitchenArea != null ||
            flat.floor != null ||
            !flat.bathroomType.isNullOrBlank() ||
            !flat.balcony.isNullOrBlank() ||
            !flat.repairType.isNullOrBlank() ||
            flat.windowDirection.isNotEmpty() ||
            flat.sleepingPlaces != null
}

private fun hasBuildingData(flat: UiDetailFlat): Boolean {
    return flat.totalFloors != null ||
            flat.yearBuilt != null ||
            flat.buildingImprovements.isNotEmpty() ||
            !flat.parkingInfo.isNullOrBlank()
}

@Composable
private fun PublicationInfo(
    timeAgo: String?,
    isOwner: Boolean?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        timeAgo?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        isOwner?.let {
            Text(
                text = if (it) "Собственник" else "Агент",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SourceLinkSection(
    platform: FlatPlatform,
    url: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Text(
        text = "Посмотреть объявление на ${platform.value.capitalize()}",
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri(url)
            }
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline
    )
}

@Composable
private fun PriceSection(priceUsd: String, priceByn: String, priceUsdSquare: String?, priceBynSquare: String?) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = priceUsd,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            priceUsdSquare?.let {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "($it)",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = priceByn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            priceBynSquare?.let {
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun AddressSection(address: String, metroStation: String?) {
    Column {
        if (address.isNotBlank()) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        metroStation?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun hasContactData(contactInfo: ContactInformationUi?): Boolean {
    return contactInfo != null && (!contactInfo.ownerName.isNullOrBlank() || !contactInfo.phones.isNullOrEmpty())
}

@Composable
private fun ContactInfoSection(contactInfo: ContactInformationUi?) {
    // Return early if there's no information to display.
    if (contactInfo == null || (contactInfo.ownerName.isNullOrBlank() && contactInfo.phones.isNullOrEmpty())) {
        return
    }

    val uriHandler = LocalUriHandler.current

    SectionCard(title = "Контактная информация") {
        // Display the owner's name if available
        contactInfo.ownerName?.takeIf { it.isNotBlank() }?.let { name ->
            InfoRow(label = "Контактное лицо", value = name)
        }

        // Display each phone number, making it clickable
        contactInfo.phones?.forEach { phone ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Телефон",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            uriHandler.openUri("tel:$phone")
                        }
                )
            }
        }
    }
}

@Composable
private fun LocationSection(district: String?, address: String?, metroStation: String?) {
    SectionCard(title = "Местоположение") {
        district?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Район", it)
        }
        address?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Адрес", it)
        }
        metroStation?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Метро", it)
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    SectionCard(title = "Описание") {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AboutBuildingSection(flat: UiDetailFlat) {
    SectionCard(title = "О доме") {
        flat.totalFloors?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Этажность дома", it)
        }
        flat.yearBuilt?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Год постройки", it)
        }
        flat.parkingInfo?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Парковка", it)
        }
        if (flat.buildingImprovements.isNotEmpty()) {
            InfoRow("Инфраструктура", flat.buildingImprovements.joinToString(", "))
        }
    }
}

@Composable
private fun AboutApartmentSection(flat: UiDetailFlat) {
    SectionCard(title = "О квартире") {
        if (flat.numberOfRooms.isNotBlank()) {
            InfoRow("Количество комнат", flat.numberOfRooms)
        }
        flat.totalArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Общая площадь", it)
        }
        flat.livingArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Жилая площадь", it)
        }
        flat.kitchenArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Площадь кухни", it)
        }
        flat.floor?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Этаж", it)
        }
        flat.bathroomType?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Санузел", it)
        }
        flat.balcony?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Балкон/лоджия", it)
        }
        flat.repairType?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Ремонт", it)
        }
        if (flat.windowDirection.isNotEmpty()) {
            InfoRow("Окна выходят", flat.windowDirection.joinToString(", "))
        }
        flat.sleepingPlaces?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Спальных мест", it)
        }
    }
}

@Composable
private fun AmenitiesSection(
    amenities: List<String>,
    kitchenEquipment: List<String>
) {
    SectionCard(title = "Удобства и оборудование") {
        if (amenities.isNotEmpty()) {
            InfoRow("В квартире", amenities.joinToString(", "))
        }
        if (kitchenEquipment.isNotEmpty()) {
            InfoRow("На кухне", kitchenEquipment.joinToString(", "))
        }
    }
}

@Composable
private fun ConditionsSection(
    forWhom: List<String>?,
    prepaymentType: String?
) {
    SectionCard(title = "Условия проживания") {
        forWhom?.takeIf { it.isNotEmpty() }?.let {
            InfoRow("Для кого", it.joinToString(", "))
        }
        prepaymentType?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Предоплата", it)
        }
    }
}

@Composable
private fun QuickInfoSection(
    rooms: String,
    year: String?,
    area: String?,
    condition: String?
) {
    val info = buildString {
        if (rooms.isNotBlank()) {
            append(rooms)
            if (rooms != "Студия") append(" комн")
        }
        year?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append("$it год")
        }
        area?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
        condition?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
    }

    if (info.isNotBlank()) {
        Text(
            text = info,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}