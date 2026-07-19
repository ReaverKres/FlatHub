package io.flatzen.commoncomponents.utils

interface DevicePlatform {
    val platformType: PlatformType
    val deviceInfo: String
    val deviceId: String

    /** Total physical RAM in bytes. */
    fun totalRamBytes(): Long

    /** Free space on the app-usable volume in bytes. */
    fun freeDiskBytes(): Long
}

enum class PlatformType {
    ANDROID,
    IOS
}
