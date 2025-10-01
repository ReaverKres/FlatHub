package io.flatzen.commoncomponents.utils

import platform.UIKit.UIDevice

actual class DevicePlatform actual constructor() {
    actual val platformType: PlatformType = PlatformType.IOS
    
    actual val deviceInfo: String
        get() = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion} - ${UIDevice.currentDevice.model}"
}