package io.flatzen.utils

import androidx.compose.runtime.Composable
import io.flatzen.commoncomponents.commonentities.Coordinates

@Composable
expect fun mapLauncher(
    onMapLaunchError: (exception: Exception) -> Unit = {}
): MapLauncher

interface MapLauncher {
    fun launchMaps(coordinates: Coordinates)
}