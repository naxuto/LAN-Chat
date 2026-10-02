package com.lanchat.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress
import java.nio.ByteOrder

data class NetworkStatus(
    val isWifiConnected: Boolean = false,
    val localIp: String = "0.0.0.0",
    val gatewayIp: String = "0.0.0.0",
    val isLanAvailable: Boolean = false
)

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus

    init {
        registerNetworkCallback()
        updateNetworkStatus()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkStatus()
            }

            override fun onLost(network: Network) {
                updateNetworkStatus()
            }
        })
    }

    @Suppress("DEPRECATION")
    fun updateNetworkStatus() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo

        val isConnected = wifiManager.connectionInfo.networkId != -1
        val ipAddress = formatIpAddress(dhcpInfo.ipAddress)
        val gateway = formatIpAddress(dhcpInfo.gateway)

        _networkStatus.value = NetworkStatus(
            isWifiConnected = isConnected,
            localIp = ipAddress,
            gatewayIp = gateway,
            isLanAvailable = isConnected && ipAddress != "0.0.0.0"
        )
    }

    private fun formatIpAddress(ip: Int): String {
        val byteOrder = if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ip else Integer.reverseBytes(ip)
        return try {
            val bytes = byteArrayOf(
                (byteOrder and 0xff).toByte(),
                (byteOrder shr 8 and 0xff).toByte(),
                (byteOrder shr 16 and 0xff).toByte(),
                (byteOrder shr 24 and 0xff).toByte()
            )
            InetAddress.getByAddress(bytes).hostAddress ?: "0.0.0.0"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }
}
