package io.flatzen.utils

import android.app.Application
import android.widget.Toast
import io.flatzen.FlatZenApp

actual open class ToastLauncher actual constructor() {
    actual fun showToast(
        message: String,
        toastDurationType: ToastDurationType
    ) {
        val context = FlatZenApp.instance
        val duration = when (toastDurationType) {
            ToastDurationType.SHORT -> Toast.LENGTH_SHORT
            ToastDurationType.LONG -> Toast.LENGTH_LONG
        }
        context?.let {
            Toast.makeText(context, message, duration).show()
        }

    }
}