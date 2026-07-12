package io.flatzen.monetization.billing

import android.app.Activity
import java.lang.ref.WeakReference

object CurrentActivityHolder {
    private var activityRef: WeakReference<Activity>? = null

    var activity: Activity?
        get() = activityRef?.get()
        set(value) {
            activityRef = value?.let { WeakReference(it) }
        }
}
