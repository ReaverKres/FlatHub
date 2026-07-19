package io.flatzen.commoncomponents.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

class DevicePlatformImpl : DevicePlatform {
    override val platformType: PlatformType = PlatformType.IOS

    override val deviceInfo: String
        get() = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion} - ${UIDevice.currentDevice.model}"

    override val deviceId: String =
        UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_vendor_id"

    override fun totalRamBytes(): Long =
        NSProcessInfo.processInfo.physicalMemory.toLong()

    @OptIn(ExperimentalForeignApi::class)
    override fun freeDiskBytes(): Long {
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(
            path = NSHomeDirectory(),
            error = null,
        ) ?: return Long.MAX_VALUE
        return (attrs[NSFileSystemFreeSize] as? NSNumber)?.longValue ?: Long.MAX_VALUE
    }
}
