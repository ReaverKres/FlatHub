package io.flatzen.commoncomponents.network

import kotlinx.coroutines.flow.Flow

interface ConnectionMonitor {
    fun isWifiAvailable(): Boolean

    fun isCellularAvailable(): Boolean

    fun isVpnConnected(): Boolean

    val isNetworkAvailable: Flow<Boolean>
}