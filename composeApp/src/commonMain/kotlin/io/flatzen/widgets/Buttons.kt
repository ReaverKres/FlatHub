package io.flatzen.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.utils.mapLauncher

@Composable
fun OpenInMapButton(
    coordinates: Coordinates,
    modifier: Modifier = Modifier,
    buttonText: String = "Открыть на карте",
    onMapLaunchError: (exception: Exception) -> Unit = {}
) {
    val mapLauncher = mapLauncher(onMapLaunchError)

    Button(
        onClick = {
            mapLauncher.launchMaps(coordinates)
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Blue.copy(alpha = 0.8f),
            contentColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Text(text = buttonText)
    }
}

@Composable
fun RentSaleButtons(
    selectedAdType: AdType,
    onClick: (AdType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                onClick(AdType.RENT)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedAdType == AdType.RENT) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selectedAdType == AdType.RENT) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
//            Icon(
//                imageVector = rentIcon,
//                contentDescription = null,
//                modifier = Modifier.padding(end = 8.dp)
//            )
//            Spacer(Modifier.width(6.dp))
            Text("Аренда")
        }

        Spacer(Modifier.width(16.dp))

        Button(
            onClick = {
                onClick(AdType.SALE)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedAdType == AdType.SALE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selectedAdType == AdType.SALE) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
//            Icon(
//                imageVector = saleIcon,
//                contentDescription = null,
//                modifier = Modifier.padding(end = 8.dp)
//            )
//            Spacer(Modifier.width(6.dp))
            Text("Продажа")
        }
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
                text = "По новизне",
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
                text = "Сначала дешевле",
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
                text = "Сначала дороже",
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
    onClick: () -> Unit
) {
    TextButton(
        colors = ButtonDefaults.textButtonColors()
            .copy(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
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