package io.flatzen.commoncomponents.utils

interface DevicePlatform {
    val platformType: PlatformType
    val deviceInfo: String
    val deviceId: String
}

enum class PlatformType {
    ANDROID,
    IOS
}