package io.flatzen.utils

import android.content.Context
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual open class ToastLauncher actual constructor() : KoinComponent {
    private val context: Context by inject()

    actual fun showToast(
        message: String,
        toastDurationType: ToastDurationType
    ) {
        val duration = when (toastDurationType) {
            ToastDurationType.SHORT -> Toast.LENGTH_SHORT
            ToastDurationType.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, message, duration).show()
    }
}
