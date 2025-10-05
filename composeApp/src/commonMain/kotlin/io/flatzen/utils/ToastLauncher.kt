package io.flatzen.utils

enum class ToastDurationType {
    SHORT,
    LONG
}

expect open class ToastLauncher() {
    fun showToast(message: String, toastDurationType: ToastDurationType)
}