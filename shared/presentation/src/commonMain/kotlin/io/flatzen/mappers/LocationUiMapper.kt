package io.flatzen.mappers

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.Coordinates

object LocationUiMapper {
    data class UiCityItem(val code: CityCode, val displayName: String, val coordinates: Coordinates)

    val minskUiItem = UiCityItem(CityCode.MINSK, "Минск", Coordinates(53.902147, 27.561388))

    fun cities(): List<UiCityItem> = listOf(
        minskUiItem,
        UiCityItem(CityCode.BREST, "Брест", Coordinates(52.093825, 23.684889)),
        UiCityItem(CityCode.VITEBSK, "Витебск", Coordinates(55.184217, 30.202878)),
        UiCityItem(CityCode.GOMEL, "Гомель", Coordinates(52.424160, 31.014281)),
        UiCityItem(CityCode.GRODNO, "Гродно", Coordinates(53.677839, 23.829529)),
        UiCityItem(CityCode.MOGILEV, "Могилёв", Coordinates(53.894548, 30.330654)),
    )

    fun findSelectedCity(cityCode: CityCode): UiCityItem = cities().find { it.code == cityCode } ?: minskUiItem

}