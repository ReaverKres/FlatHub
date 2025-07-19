package io.flatzen

import android.app.Application
import io.flatzen.di.initKoin

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
