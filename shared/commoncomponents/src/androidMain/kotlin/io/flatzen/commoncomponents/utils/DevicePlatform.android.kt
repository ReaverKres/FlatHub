package io.flatzen.commoncomponents.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.provider.Settings

class DevicePlatformImpl(private val context: Context) : DevicePlatform {

    override val platformType: PlatformType = PlatformType.ANDROID

    override val deviceInfo: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MANUFACTURER} ${Build.MODEL}"

    @SuppressLint("HardwareIds")
    override val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    override fun totalRamBytes(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

    override fun freeDiskBytes(): Long =
        StatFs(context.cacheDir.absolutePath).availableBytes
}
