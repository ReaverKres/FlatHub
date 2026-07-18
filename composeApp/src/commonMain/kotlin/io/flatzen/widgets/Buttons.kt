package io.flatzen.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.detail_open_in_map
import flatzen.composeapp.generated.resources.filter_commercial_rent
import flatzen.composeapp.generated.resources.filter_commercial_sale
import flatzen.composeapp.generated.resources.filter_daily
import flatzen.composeapp.generated.resources.filter_rent
import flatzen.composeapp.generated.resources.filter_sale
import flatzen.composeapp.generated.resources.sort_cheapest
import flatzen.composeapp.generated.resources.sort_expensive
import flatzen.composeapp.generated.resources.sort_newest
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.AdType.COMMERCIAL
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.themes.FlatHubTheme
import io.flatzen.utils.mapLauncher
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpenInMapButton(
    coordinates: Coordinates,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onMapLaunchError: (exception: Exception) -> Unit = {}
) {
    val mapLauncher = mapLauncher(onMapLaunchError)

    Button(
        onClick = {
            mapLauncher.launchMaps(coordinates)
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
    ) {
        Text(text = buttonText ?: stringResource(Res.string.detail_open_in_map))
    }
}

@Composable
fun RentSaleButtons(
    selectedAdType: AdType,
    lastCommercialAdType: AdType,
    onClick: (AdType) -> Unit,
    fewTypeInOneClick: (AdType) -> Unit,
    showRent: Boolean = true,
    showSale: Boolean = true,
    showDaily: Boolean = true,
    showCommercial: Boolean = true,
) {

    val commercialAdTypeBtnText = when (lastCommercialAdType) {
        COMMERCIAL(CommercialAdType.RENT) -> stringResource(Res.string.filter_commercial_rent)
        COMMERCIAL(CommercialAdType.SALE) -> stringResource(Res.string.filter_commercial_sale)
        else -> stringResource(Res.string.filter_commercial_rent)
    }

    Column {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showRent) {
                Row {
                    AdTypeButton(
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1f, fill = false),
                        adType = AdType.RENT,
                        adTypeBtnText = stringResource(Res.string.filter_rent),
                        onClick = onClick,
                        selectedAdType = selectedAdType
                    )

                    Spacer(Modifier.width(16.dp))
                }
            }

            if (showSale) {
                Row {
                    AdTypeButton(
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1f, fill = false),
                        adType = AdType.SALE,
                        adTypeBtnText = stringResource(Res.string.filter_sale),
                        onClick = onClick,
                        selectedAdType = selectedAdType
                    )
                    Spacer(Modifier.width(16.dp))
                }
            }

            if (showDaily) {
                Row {
                    AdTypeButton(
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1f, fill = false),
                        adType = AdType.DAILY,
                        adTypeBtnText = stringResource(Res.string.filter_daily),
                        onClick = onClick,
                        selectedAdType = selectedAdType
                    )
                    Spacer(Modifier.width(16.dp))
                }
            }
            if (showCommercial) {
                Row(
                    modifier = Modifier.wrapContentWidth()
                ) {
                    AdTypeButton(
                        modifier = Modifier
                            .height(44.dp)
                            .wrapContentWidth(),
                        adType = lastCommercialAdType,
                        adTypeBtnText = commercialAdTypeBtnText,
                        onClick = onClick,
                        selectedAdType = selectedAdType
                    )
                    Spacer(Modifier.width(8.dp))
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.wrapContentWidth().height(44.dp),
                        onClick = { fewTypeInOneClick(AdType.COMMERCIAL()) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            modifier = Modifier.padding(12.dp),
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = FlatHubTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
    ) {
//            Icon(
//                imageVector = rentIcon,
//                contentDescription = null,
//                modifier = Modifier.padding(end = 8.dp)
//            )
//            Spacer(Modifier.width(6.dp))
        Text(text = text, maxLines = 1)
    }
}

@Composable
private fun AdTypeButton(
    modifier: Modifier,
    adType: AdType,
    adTypeBtnText: String,
    onClick: (AdType) -> Unit,
    selectedAdType: AdType
) {
    Button(
        onClick = {
            onClick(adType)
        },
        shape = FlatHubTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedAdType == adType) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selectedAdType == adType) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Text(
            text = adTypeBtnText,
            maxLines = 1,
            color = if (selectedAdType == adType) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun SortOptionRadioButtons(
    selectedSortOption: FlatSort,
    onClick: (FlatSort) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(FlatSort.NEWEST_FIRST) }
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = selectedSortOption == FlatSort.NEWEST_FIRST,
                onClick = { onClick(FlatSort.NEWEST_FIRST) }
            )
            Text(
                text = stringResource(Res.string.sort_newest),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(FlatSort.CHEAPEST_FIRST) }
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = selectedSortOption == FlatSort.CHEAPEST_FIRST,
                onClick = { onClick(FlatSort.CHEAPEST_FIRST) }
            )
            Text(
                text = stringResource(Res.string.sort_cheapest),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(FlatSort.MOST_EXPENSIVE_FIRST) }
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = selectedSortOption == FlatSort.MOST_EXPENSIVE_FIRST,
                onClick = { onClick(FlatSort.MOST_EXPENSIVE_FIRST) }
            )
            Text(
                text = stringResource(Res.string.sort_expensive),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AppTextButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    image: Any?,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    TextButton(
        colors = ButtonDefaults.textButtonColors()
            .copy(contentColor = contentColor),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            image?.let {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = style
            )
        }
    }
}