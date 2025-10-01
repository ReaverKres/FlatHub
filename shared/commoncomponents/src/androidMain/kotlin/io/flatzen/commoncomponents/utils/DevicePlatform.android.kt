package io.flatzen.commoncomponents.utils

import android.os.Build

actual class DevicePlatform actual constructor() {
    actual val platformType: PlatformType = PlatformType.ANDROID
    
    actual val deviceInfo: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MANUFACTURER} ${Build.MODEL}"
}