package io.flatzen.mappers

import io.flatzen.states.UiCity

object LocationUiMapper {
    data class UiCityItem(val code: String, val displayName: String)

    fun cities(): List<UiCityItem> = listOf(
        UiCityItem("MINSK", "Минск"),
        UiCityItem("BREST", "Брест"),
        UiCityItem("VITEBSK", "Витебск"),
        UiCityItem("GOMEL", "Гомель"),
        UiCityItem("GRODNO", "Гродно"),
        UiCityItem("MOGILEV", "Могилёв"),
    )

    fun displayName(code: String): String = cities().firstOrNull { it.code == code }?.displayName ?: code
}