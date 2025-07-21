package io.flatzen

import io.flatzen.di.initKoin

object CommonApplication {
    fun initialize() {
        initKoin()
    }
}