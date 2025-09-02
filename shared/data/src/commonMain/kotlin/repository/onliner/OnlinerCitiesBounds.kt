package repository.onliner

import entities.Coordinates

object OnlinerCitiesBounds {
    // Брест
    val BREST = Bounds(
        southwest = Coordinates(latitude = 51.87140380383993, longitude = 23.53202819824219),
        northeast = Coordinates(latitude = 52.3042802056519, longitude = 23.887710571289066)
    )

    // Витебск
    val VITEBSK = Bounds(
        southwest = Coordinates(latitude = 55.085834940707, longitude = 29.979629516602),
        northeast = Coordinates(latitude = 55.357648391381, longitude = 30.414276123047)
    )

    // Гомель
    val GOMEL = Bounds(
        southwest = Coordinates(latitude = 52.302600726968, longitude = 30.732192993164),
        northeast = Coordinates(latitude = 52.593037841157, longitude = 31.166839599609)
    )

    // Гродно
    val GRODNO = Bounds(
        southwest = Coordinates(latitude = 53.538267122397, longitude = 23.629531860352),
        northeast = Coordinates(latitude = 53.820517109806, longitude = 24.064178466797)
    )

    // Могилёв
    val MOGILEV = Bounds(
        southwest = Coordinates(latitude = 53.74261986683, longitude = 30.132064819336),
        northeast = Coordinates(latitude = 54.023503252809, longitude = 30.566711425781)
    )

    // Минск
    val MINSK = Bounds(
        southwest = Coordinates(latitude = 53.820922446131, longitude = 27.344970703125),
        northeast = Coordinates(latitude = 53.97547425743, longitude = 27.77961730957)
    )
}

data class Bounds(
    val southwest: Coordinates, // lb - left bottom
    val northeast: Coordinates  // rt - right top
)