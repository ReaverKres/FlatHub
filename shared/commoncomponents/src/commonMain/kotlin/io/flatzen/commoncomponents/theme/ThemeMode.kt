package io.flatzen.commoncomponents.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}
