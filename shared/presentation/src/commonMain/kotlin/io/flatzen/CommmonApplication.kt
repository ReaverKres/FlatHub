package io.flatzen

import io.flatzen.data.di.initKoin

object CommonApplication {
    fun initialize() {
        initKoin()
    }
}