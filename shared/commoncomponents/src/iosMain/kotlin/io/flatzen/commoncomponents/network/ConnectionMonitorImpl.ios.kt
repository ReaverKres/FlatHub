package io.flatzen.commoncomponents.network

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CoreFoundation.CFDictionaryGetCount
import platform.CoreFoundation.CFDictionaryGetKeysAndValues
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringGetCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_other
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_uses_interface_type
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.DISPATCH_QUEUE_SERIAL
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_attr_t
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_UTILITY

private val vpnMarkers = listOf("tap", "tun", "ppp", "ipsec", "utun")

@OptIn(ExperimentalForeignApi::class)
internal class ConnectionMonitorImpl : ConnectionMonitor {

    private enum class ConnectivityType {
        WiFi, Cellular, None, Unknown
    }

    private val connectivity = MutableStateFlow(ConnectivityType.Unknown)
    override val isNetworkAvailable: Flow<Boolean>
        get() = connectivity.map { type ->
            type == ConnectivityType.WiFi || type == ConnectivityType.Cellular
        }

    init {
        val dispatchQueueSerial: dispatch_queue_attr_t = DISPATCH_QUEUE_SERIAL as NSObject?
        val attrs = dispatch_queue_attr_make_with_qos_class(dispatchQueueSerial, QOS_CLASS_UTILITY, DISPATCH_QUEUE_PRIORITY_DEFAULT)

        val queue = dispatch_queue_create("com.example.network-monitor", attrs);
        val monitor = nw_path_monitor_create()

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            connectivity.value = when {
                nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectivityType.WiFi
                nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectivityType.Cellular
                nw_path_uses_interface_type(path, nw_interface_type_other) -> ConnectivityType.Unknown
                nw_path_uses_interface_type(path, nw_interface_type_wired) -> ConnectivityType.Unknown
                else -> ConnectivityType.None
            }
        }
        nw_path_monitor_start(monitor)
    }

    override fun isWifiAvailable(): Boolean {
        return connectivity.value == ConnectivityType.WiFi
    }

    override fun isCellularAvailable(): Boolean {
        return connectivity.value == ConnectivityType.Cellular
    }

    override fun isVpnConnected(): Boolean {
        val settings = CFNetworkCopySystemProxySettings() ?: return false

        val scopedKey =
            CFStringCreateWithCString(null, "__SCOPED__", kCFStringEncodingUTF8) ?: return false
        val scopedRaw = CFDictionaryGetValue(settings, scopedKey) ?: return false
        val scoped = scopedRaw as? CFDictionaryRef ?: return false

        val count = CFDictionaryGetCount(scoped).toInt()
        if (count == 0) return false

        memScoped {
            val keys = allocArray<COpaquePointerVar>(count)
            CFDictionaryGetKeysAndValues(scoped, keys, null)

            repeat(count) { i ->
                val keyPtr = keys[i] ?: return@repeat
                val cfStr = keyPtr as? CFStringRef ?: return@repeat

                val bufSize = 256
                val buf = allocArray<ByteVar>(bufSize)
                val ok = CFStringGetCString(cfStr, buf, bufSize.toLong(), kCFStringEncodingUTF8)
                if (!ok) return@repeat

                val key = buf.toKString()
                if (vpnMarkers.any { marker -> key.contains(marker, ignoreCase = true) }) {
                    return true
                }
            }
        }

        return false
    }
}
