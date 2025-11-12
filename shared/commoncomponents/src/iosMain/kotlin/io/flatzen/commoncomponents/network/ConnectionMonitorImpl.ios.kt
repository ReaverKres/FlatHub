//package io.flatzen.commoncomponents.network
//
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.map
//import platform.Network.nw_interface_type_cellular
//import platform.Network.nw_interface_type_other
//import platform.Network.nw_interface_type_wifi
//import platform.Network.nw_interface_type_wired
//import platform.Network.nw_path_monitor_create
//import platform.Network.nw_path_monitor_set_queue
//import platform.Network.nw_path_monitor_set_update_handler
//import platform.Network.nw_path_monitor_start
//import platform.Network.nw_path_uses_interface_type
//import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
//import platform.darwin.DISPATCH_QUEUE_SERIAL
//import platform.darwin.NSObject
//import platform.darwin.dispatch_queue_attr_make_with_qos_class
//import platform.darwin.dispatch_queue_attr_t
//import platform.darwin.dispatch_queue_create
//import platform.posix.QOS_CLASS_UTILITY
//
//@kotlinx.cinterop.ExperimentalForeignApi
//internal actual class ConnectionMonitorImpl : ConnectionMonitor {
//
//    private enum class ConnectivityType {
//        WiFi, Cellular, None, Unknown
//    }
//
//    private val connectivity = MutableStateFlow(ConnectivityType.Unknown)
//    override val isNetworkAvailable: Flow<Boolean>
//        get() = connectivity.map { type ->
//            type == ConnectivityType.WiFi || type == ConnectivityType.Cellular
//        }
//
//    init {
//        val dispatchQueueSerial: dispatch_queue_attr_t = DISPATCH_QUEUE_SERIAL as NSObject?
//        val attrs = dispatch_queue_attr_make_with_qos_class(dispatchQueueSerial, QOS_CLASS_UTILITY, DISPATCH_QUEUE_PRIORITY_DEFAULT)
//
//        val queue = dispatch_queue_create("com.example.network-monitor", attrs);
//        val monitor = nw_path_monitor_create()
//
//        nw_path_monitor_set_queue(monitor, queue)
//        nw_path_monitor_set_update_handler(monitor) { path ->
//            connectivity.value = when {
//                nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectivityType.WiFi
//                nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectivityType.Cellular
//                nw_path_uses_interface_type(path, nw_interface_type_other) -> ConnectivityType.Unknown
//                nw_path_uses_interface_type(path, nw_interface_type_wired) -> ConnectivityType.Unknown
//                else -> ConnectivityType.None
//            }
//        }
//        nw_path_monitor_start(monitor)
//    }
//
//    override fun isWifiAvailable(): Boolean {
//        return connectivity.value == ConnectivityType.WiFi
//    }
//
//    override fun isCellularAvailable(): Boolean {
//        return connectivity.value == ConnectivityType.Cellular
//    }
//}