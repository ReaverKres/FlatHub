import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.screens.list.ImagePager
import io.flatzen.screens.list.LoadingContent
import io.flatzen.viewmodel.FlatDetailScreenAction
import io.flatzen.viewmodel.FlatDetailViewModel
import io.flatzen.viewmodel.UiDetailFlat
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    flatPlatform: FlatPlatform,
    objectId: Long,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<FlatDetailViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(objectId) {
        viewModel.onIntent(FlatDetailScreenAction.LoadFlatDetails(flatPlatform, objectId))
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Детали квартиры") },
            navigationIcon = {
                IconButton(onClick = navigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        when {
            state.isLoading -> {
                LoadingContent(modifier = Modifier.fillMaxSize())
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
                    modifier = Modifier.fillMaxSize()
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Изображения
        ImagePager(imageUrls = flat.imageUrls)

        // Основная информация
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о публикации
            PublicationInfo(
                timeAgo = flat.timeAgo,
                isOwner = flat.isOwner
            )

            SourceLinkSection(flat.platform, flat.flatUrl)

            // Цены
            PriceSection(
                priceUsd = flat.priceUsd,
                priceByn = flat.priceByn
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

            HorizontalDivider()

            // О квартире
            AboutApartmentSection(flat = flat)

            HorizontalDivider()

            // Удобства и оборудование
            if (flat.amenities.isNotEmpty() || flat.kitchenEquipment.isNotEmpty()) {
                AmenitiesSection(
                    amenities = flat.amenities,
                    kitchenEquipment = flat.kitchenEquipment
                )
                HorizontalDivider()
            }

            // Условия проживания
            if (!flat.forWhom.isNullOrEmpty() || flat.prepaymentType != null) {
                ConditionsSection(
                    forWhom = flat.forWhom,
                    prepaymentType = flat.prepaymentType
                )
                HorizontalDivider()
            }

            // О доме
            AboutBuildingSection(flat = flat)

            HorizontalDivider()

            // Местоположение
            LocationSection(
                district = flat.district,
                metroStation = flat.metroStation
            )

            HorizontalDivider()

            // Описание
            DescriptionSection(description = flat.description)
        }
    }
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
private fun PriceSection(priceUsd: String, priceByn: String) {
    Column {
        Text(
            text = priceUsd,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = priceByn,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun AddressSection(address: String, metroStation: String?) {
    Column {
        Text(
            text = address,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
        metroStation?.let {
            Text(
                text = "🚇 $it",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun LocationSection(district: String?, metroStation: String?) {
    if (district != null || metroStation != null) {
        SectionCard(title = "Местоположение") {
            district?.let {
                InfoRow("Район", it)
            }
            metroStation?.let {
                InfoRow("Метро", it)
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    if (description.isNotEmpty()) {
        SectionCard(title = "Описание") {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
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
    val hasData = flat.totalFloors != null ||
            flat.yearBuilt != null ||
            flat.buildingImprovements.isNotEmpty() ||
            flat.parkingInfo != null

    if (hasData) {
        SectionCard(title = "О доме") {
            flat.totalFloors?.let {
                InfoRow("Этажность дома", it)
            }
            flat.yearBuilt?.let {
                InfoRow("Год постройки", it)
            }
            flat.parkingInfo?.let {
                InfoRow("Парковка", it)
            }
            if (flat.buildingImprovements.isNotEmpty()) {
                InfoRow("Инфраструктура", flat.buildingImprovements.joinToString(", "))
            }
        }
    }
}

@Composable
private fun AboutApartmentSection(flat: UiDetailFlat) {
    SectionCard(title = "О квартире") {
        InfoRow("Количество комнат", flat.numberOfRooms)
        flat.totalArea?.let {
            InfoRow("Общая площадь", it)
        }
        flat.livingArea?.let {
            InfoRow("Жилая площадь", it)
        }
        flat.kitchenArea?.let {
            InfoRow("Площадь кухни", it)
        }
        flat.floor?.let {
            InfoRow("Этаж", it)
        }
        flat.bathroomType?.let {
            InfoRow("Санузел", it)
        }
        flat.balcony?.let {
            InfoRow("Балкон/лоджия", it)
        }
        flat.repairType?.let {
            InfoRow("Ремонт", it)
        }
        if (flat.windowDirection.isNotEmpty()) {
            InfoRow("Окна выходят", flat.windowDirection.joinToString(", "))
        }
        flat.sleepingPlaces?.let {
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
        forWhom?.let {
            if (it.isNotEmpty()) {
                InfoRow("Для кого", it.joinToString(", "))
            }
        }
        prepaymentType?.let {
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
    Text(
        text = buildString {
            append(rooms)
            if (rooms != "Студия") append(" комн")
            year?.let { append(" • $it год") }
            area?.let { append(" • $it") }
            condition?.let { append(" • $it") }
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}