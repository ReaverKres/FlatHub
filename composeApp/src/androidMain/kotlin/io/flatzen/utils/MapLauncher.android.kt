package io.flatzen.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.flatzen.commoncomponents.commonentities.Coordinates

@Composable
actual fun mapLauncher(onMapLaunchError: (Exception) -> Unit): MapLauncher {
    val context = LocalContext.current
    return remember {
        AndroidMapLauncher(context, onMapLaunchError)
    }
}

class AndroidMapLauncher(
    private val context: Context,
    private val onMapLaunchError: (exception: Exception) -> Unit
) : MapLauncher {
    override fun launchMaps(coordinates: Coordinates) {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude
        val geoUriString = "geo:$latitude,$longitude?q=$latitude,$longitude"
        val gmmIntentUri = geoUriString.toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                // Если не удалось запустить нативное приложение (маловероятно, если resolveActivity не null, но для безопасности)
                onMapLaunchError(e)
                openMapsInBrowser(coordinates)
            }
        } else {
            // Если ни одно приложение не установлено, открываем в браузере
            openMapsInBrowser(coordinates)
        }
    }

    private fun openMapsInBrowser(coordinates: Coordinates) {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude
        // Формируем URL для Google Maps, который хорошо работает в большинстве браузеров
        val browserUriString = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        val browserIntent = Intent(Intent.ACTION_VIEW, browserUriString.toUri())
        try {
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            onMapLaunchError(e)
        }
    }
}