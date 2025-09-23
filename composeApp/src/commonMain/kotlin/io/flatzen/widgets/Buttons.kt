package io.flatzen.widgets

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.flatzen.commoncomponents.commonentities.Coordinates
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