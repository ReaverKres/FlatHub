package io.flatzen.commoncomponents.utils

import platform.UIKit.UIDevice

class DevicePlatformImpl: DevicePlatform {
    override val platformType: PlatformType = PlatformType.IOS

    override val deviceInfo: String
        get() = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion} - ${UIDevice.currentDevice.model}"

    override val deviceId: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_vendor_id"
}