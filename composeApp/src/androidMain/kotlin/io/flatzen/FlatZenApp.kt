package io.flatzen

import android.app.Application
import org.koin.android.ext.koin.androidContext

class FlatZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CommonApplication.initialize {
            androidContext(applicationContext)
        }
    }
}
