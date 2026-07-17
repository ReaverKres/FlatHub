package io.flatzen.commoncomponents.theme

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    EN("en"),
    ES("es"),
    RU("ru"),
    PL("pl"),
    KK("kk"),
    KA("ka");

    companion object {
        fun fromStored(value: String?): AppLanguage =
            entries.find { it.name == value } ?: SYSTEM
    }
}
