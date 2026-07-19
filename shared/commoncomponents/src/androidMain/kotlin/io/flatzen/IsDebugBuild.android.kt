package io.flatzen

import android.content.pm.ApplicationInfo

actual val isDebugBuild: Boolean by lazy {
    try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val app = activityThread
            .getMethod("currentApplication")
            .invoke(null) as? android.content.Context
            ?: return@lazy true
        (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (_: Throwable) {
        true
    }
}
