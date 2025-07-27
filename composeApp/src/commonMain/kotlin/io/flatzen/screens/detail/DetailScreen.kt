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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextAlign
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
import io.flatzen.viewmodel.UiAdditionalParams
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
        // TopAppBar
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
//                ErrorContent(
//                    error = state.error,
//                    onRetry = {
//                        viewModel.onIntent(FlatDetailScreenAction.LoadFlatDetails(objectId.toString()))
//                    },
//                    modifier = Modifier.fillMaxSize()
//                )
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

            SourceLinkSection(flat.platform, flat.flatUrl)
            // Цены
            PriceSection(
                priceUsd = flat.priceUsd,
                priceByn = flat.priceByn
            )

            // Адрес
            AddressSection(address = flat.address)

            // Краткая информация
            QuickInfoSection(
                rooms = flat.numberOfRooms,
                year = flat.yearBuilt.orEmpty(),
                area = flat.totalArea
            )

            HorizontalDivider()

            // О квартире
            AboutApartmentSection(flat = flat)

            HorizontalDivider()

            flat.additionalParams?.let {
                AdditionalParamsSection(it)
                HorizontalDivider()
            }

            // О доме
            AboutBuildingSection(
                totalFloors = flat.totalFloors,
                year = flat.yearBuilt
            )

            HorizontalDivider()

            // Местоположение
            LocationSection(district = flat.district)

            HorizontalDivider()

            // Описание
            DescriptionSection(description = flat.description)
        }
    }
}

@Composable
private fun SourceLinkSection(
    platform: FlatPlatform,
    url: String,
    modifier: Modifier = Modifier
) {
    // Получаем обработчик для открытия ссылок
    val uriHandler = LocalUriHandler.current

    Text(
        text = "Посмотреть объявление на ${platform.value.capitalize()}",
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                // Открываем ссылку при нажатии
                uriHandler.openUri(url)
            }
            .padding(vertical = 16.dp), // Добавим отступ для красоты
        color = MaterialTheme.colorScheme.primary, // Выделяем цветом
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline, // Подчеркиваем как ссылку
//        textAlign = TextAlign.Center // Располагаем по центру
    )
}

@Composable
private fun PriceSection(priceUsd: String, priceByn: String) {
    Column {
        Text(
            text = "$$priceUsd",
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
private fun AddressSection(address: String) {
    Text(
        text = address,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium
        )
    )
}

@Composable
private fun LocationSection(district: String) {
    SectionCard(title = "Местоположение") {
        if (district.isNotEmpty()) {
            InfoRow("Микрорайон", district)
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
            style = MaterialTheme.typography.bodyMedium,
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
private fun AboutBuildingSection(totalFloors: String?, year: String?) {
    SectionCard(title = "О доме") {
        totalFloors?.let {
            InfoRow("Этажность дома", it)
        }
        year?.let {
            InfoRow("Год постройки", it)
        }
    }
}

@Composable
private fun AboutApartmentSection(flat: UiDetailFlat) {
    SectionCard(title = "О квартире") {
        InfoRow("Количество комнат", "${flat.numberOfRooms}")
        flat.totalArea?.let {
            InfoRow("Общая площадь", it)
        }
        flat.floor?.let {
            InfoRow("Этаж", it)
        }
        flat.bathroomType?.let {
            InfoRow("Санузел", it)
        }
        flat.balconyType?.let {
            InfoRow("Балкон", it)
        }
        flat.repairType?.let {
            InfoRow("Ремонт", it)
        }
        if (flat.buildingImprovement.isNotEmpty()) {
            InfoRow("Обустройство", flat.buildingImprovement.joinToString(", "))
        }
        if (flat.windowDirection.isNotEmpty()) {
            InfoRow("Окна выходят", flat.windowDirection.joinToString(", "))
        }
        flat.sleepingPlaces?.let {
            InfoRow("Спальных мест", it)
        }
        if (flat.isStudio) {
            InfoRow("Тип", "Студия")
        }
        flat.prepaymentType?.let {
            InfoRow("Предоплата", it)
        }
    }
}

@Composable
private fun AdditionalParamsSection(params: UiAdditionalParams) {
    val amenities = mutableListOf<String>()
    if (params.hasFurniture) amenities.add("Мебель")
    if (params.hasWashingMachine) amenities.add("Стиральная машина")
    if (params.hasStove) amenities.add("Плита")
    if (params.hasMicrowave) amenities.add("Микроволновая печь")
    if (params.hasConditioner) amenities.add("Кондиционер")
    if (params.hasWifi) amenities.add("Wi-Fi")

    // Показываем карточку, только если есть что показать
    if (amenities.isNotEmpty() || !params.forWhom.isNullOrEmpty()) {
        SectionCard(title = "Удобства и параметры") {
            if (amenities.isNotEmpty()) {
                InfoRow("Удобства", amenities.joinToString(", "))
            }
            params.forWhom?.let {
                if (it.isNotEmpty()) {
                    InfoRow("Для кого", it.joinToString(", "))
                }
            }
        }
    }
}

@Composable
private fun QuickInfoSection(rooms: Int, year: String, area: String?) {
    Text(
        text = buildString {
            append("$rooms комн")
            append(" • $year год постройки")
            area?.let { append(" • ${it}м²") }
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}