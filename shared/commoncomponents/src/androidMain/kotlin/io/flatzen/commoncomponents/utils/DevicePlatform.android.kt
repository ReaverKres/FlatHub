package io.flatzen.commoncomponents.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

class DevicePlatformImpl(context: Context): DevicePlatform {

    override val platformType: PlatformType = PlatformType.ANDROID

    override val deviceInfo: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MANUFACTURER} ${Build.MODEL}"

    @SuppressLint("HardwareIds")
    override val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
}