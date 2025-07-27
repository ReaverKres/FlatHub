package entities

//Санузел
enum class BathroomType(val value: String) {
    //Раздельный
    SEPARATE("1"),
    //Совмещенный
    COMBINED("2")
}

//Балкон
enum class BalconyType(val value: String) {
    //Есть
    EXISTS("1"),
    //Нет
    NO("2"),
    //Лоджия
    LOGGIA("3")
}

//Ремонт
enum class RepairType(val value: String) {
    //Косметический
    COSMETIC("1"),
    //Евро
    EURO("5")
}

//Окна выходят
enum class WindowDirection(val value: String) {
    //Во двор
    YARD("1"),
    //На речку
    RIVER("2"),
    //В парк
    PARK("3"),
    //На улицу
    STREET("4"),
    //Юг
    SOUTH("5"),
    //Запад
    WEST("8")
}

////Обустройство дома
//enum class BuildingImprovement(val value: String) {
//    //Лифт
//    ELEVATOR("1"),
//    //Пандус
//    RAMP("2"),
//    //Мусоропровод
//    GARBAGE_CHUTE("3"),
//    //Стояночное место
//    PARKING("6"),
//    //Домофон
//    INTERCOM("7"),
//    //Видеонаблюдение
//    VIDEO_SURVEILLANCE("8")
//}

//Предоплата
enum class PrepaymentType(val value: String) {
    //Месяц
    MONTH("5"),
    //2 месяца
    TWO_MONTHS("10"),
    //Залог
    DEPOSIT("25")
}
//Обустройство дома
data class BuildingImprovement(val value: String) {

}