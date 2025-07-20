package io.flatzen

import android.app.Application

class FlatZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CommonApplication.initialize()
    }
}
