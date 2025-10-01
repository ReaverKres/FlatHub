package io.flatzen.commoncomponents.utils

expect class DevicePlatform() {
    val platformType: PlatformType
    val deviceInfo: String
}

enum class PlatformType {
    ANDROID,
    IOS
}