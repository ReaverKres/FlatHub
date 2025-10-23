package io.flatzen.entities

data class SingleChoiceEntity<T>(
    val title: String,
    val type: T
)
