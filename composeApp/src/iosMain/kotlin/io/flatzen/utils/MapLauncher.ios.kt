package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSURL
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark
import platform.UIKit.UIApplication

@Composable
actual fun mapLauncher(
    onMapLaunchError: (exception: Exception) -> Unit
): MapLauncher {
    return remember { // Не нужно ничего специфичного для iOS в remember, кроме самой логики
        IOSMapLauncher(onMapLaunchError)
    }
}

class IOSMapLauncher(
    private val onMapLaunchError: (exception: Exception) -> Unit
) : MapLauncher {
    @OptIn(ExperimentalForeignApi::class)
    override fun launchMaps(coordinates: Coordinates) {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude

        val appleMapsUrlString = "http://maps.apple.com/?ll=${latitude},${longitude}"
        val googleMapsUrlString = "comgooglemaps://?q=${latitude},${longitude}&center=${latitude},${longitude}&zoom=14" // Можно добавить &q=${label ?: ""}
        val yandexMapsUrlString = "yandexmaps://maps.yandex.ru/?ll=${longitude},${latitude}&z=15&pt=${longitude},${latitude},pm2rdm" // Яндекс использует lon,lat

        val appleMapsUrl = NSURL.URLWithString(appleMapsUrlString)
        val googleMapsUrl = NSURL.URLWithString(googleMapsUrlString)
        val yandexMapsUrl = NSURL.URLWithString(yandexMapsUrlString)

        val application = UIApplication.sharedApplication

        try {
            val mapCoordinates = CLLocationCoordinate2DMake(latitude, longitude)
            val placemark = MKPlacemark(coordinate = mapCoordinates, addressDictionary = null)
            val mapItem = MKMapItem(placemark = placemark)
            if (mapItem.openInMapsWithLaunchOptions(null)) {
                return
            }
            if (appleMapsUrl != null && application.canOpenURL(appleMapsUrl) && application.openURL(appleMapsUrl)) {
                return
            }
            if (googleMapsUrl != null && application.canOpenURL(googleMapsUrl) && application.openURL(googleMapsUrl)) {
                return
            }
            if (yandexMapsUrl != null && application.canOpenURL(yandexMapsUrl) && application.openURL(yandexMapsUrl)) {
                return
            }
            if (appleMapsUrl != null) {
                application.openURL(appleMapsUrl)
            } else {
                onMapLaunchError(IllegalStateException("Could not create Apple Maps URL"))
            }
        } catch (e: Exception) {
            onMapLaunchError(RuntimeException("Failed to launch maps on iOS: ${e.message}", e))
        }
    }
}