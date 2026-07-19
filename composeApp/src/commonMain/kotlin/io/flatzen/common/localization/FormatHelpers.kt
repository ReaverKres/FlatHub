package io.flatzen.common.localization

import androidx.compose.runtime.Composable
import io.flatzen.commoncomponents.localization.LocalizationKeys

@Composable
fun localizedArea(area: String, usesSquareFeet: Boolean = false): String =
    stringResource(
        if (usesSquareFeet) LocalizationKeys.LIST_AREA_FORMAT_SQFT
        else LocalizationKeys.LIST_AREA_FORMAT,
        area,
    )

@Composable
fun localizedRoomsLabel(rooms: String): String {
    val trimmed = rooms.trim()
    val count = trimmed.toIntOrNull() ?: return trimmed
    val key = when {
        count % 10 == 1 && count % 100 != 11 -> LocalizationKeys.LIST_ROOMS_ONE
        count % 10 in 2..4 && count % 100 !in 12..14 -> LocalizationKeys.LIST_ROOMS_FEW
        else -> LocalizationKeys.LIST_ROOMS_MANY
    }
    return stringResource(key, count)
}
