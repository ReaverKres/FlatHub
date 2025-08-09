package io.flatzen

import io.flatzen.di.initKoin
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

object CommonApplication {
    var di: KoinApplication? = null

    fun initialize(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration)
        }
    }
}