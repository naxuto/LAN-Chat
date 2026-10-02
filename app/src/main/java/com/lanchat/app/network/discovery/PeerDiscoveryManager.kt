package com.lanchat.app.network.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.lanchat.app.network.protocol.Packet
import com.lanchat.app.network.protocol.PacketSerializer
import com.lanchat.app.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredPeer(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val publicKey: String
)

class PeerDiscoveryManager(
    private val context: Context,
    private val userRepository: UserRepository
) {
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private var discoveryJob: Job? = null
    private var listenJob: Job? = null

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 64)
    val discoveredPeers: SharedFlow<DiscoveredPeer> = _discoveredPeers

    private var multicastLock: WifiManager.MulticastLock? = null

    fun startDiscovery(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("LanChatMulticastLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        try {
            socket = DatagramSocket(Packet.UDP_PORT).apply {
                broadcast = true
            }
        } catch (e: Exception) {
            // Port might be temporarily occupied
        }

        listenJob = scope.launch(Dispatchers.IO) {
            listenForDiscoveryPackets()
        }

        discoveryJob = scope.launch(Dispatchers.IO) {
            while (isActive && isRunning) {
                broadcastDiscovery()
                delay(5000)
            }
        }
    }

    fun stopDiscovery() {
        isRunning = false
        discoveryJob?.cancel()
        listenJob?.cancel()
        socket?.close()
        socket = null

        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
    }

    private suspend fun broadcastDiscovery() {
        try {
            val user = userRepository.getOrCreateUser()
            val packet = Packet.Discovery(deviceId = user.deviceId)
            val json = PacketSerializer.serialize(packet)
            val bytes = json.toByteArray(Charsets.UTF_8)

            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val datagram = DatagramPacket(bytes, bytes.size, broadcastAddr, Packet.UDP_PORT)
            socket?.send(datagram)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun listenForDiscoveryPackets() {
        val buffer = ByteArray(4096)
        while (isRunning) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                val s = socket ?: break
                s.receive(packet)

                val senderIp = packet.address.hostAddress ?: continue
                val rawData = String(packet.data, 0, packet.length, Charsets.UTF_8)

                when (val parsed = PacketSerializer.deserialize(rawData)) {
                    is Packet.Discovery -> {
                        val currentUser = userRepository.getOrCreateUser()
                        if (parsed.deviceId != currentUser.deviceId) {
                            val response = Packet.DiscoveryResponse(
                                deviceId = currentUser.deviceId,
                                deviceName = currentUser.displayName,
                                port = Packet.TCP_PORT,
                                publicKey = currentUser.publicKey
                            )
                            val respJson = PacketSerializer.serialize(response)
                            val respBytes = respJson.toByteArray(Charsets.UTF_8)
                            val respPacket = DatagramPacket(
                                respBytes,
                                respBytes.size,
                                packet.address,
                                Packet.UDP_PORT
                            )
                            socket?.send(respPacket)
                        }
                    }
                    is Packet.DiscoveryResponse -> {
                        val currentUser = userRepository.getOrCreateUser()
                        if (parsed.deviceId != currentUser.deviceId) {
                            _discoveredPeers.emit(
                                DiscoveredPeer(
                                    deviceId = parsed.deviceId,
                                    deviceName = parsed.deviceName,
                                    ipAddress = senderIp,
                                    port = parsed.port,
                                    publicKey = parsed.publicKey
                                )
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (!isRunning) break
            }
        }
    }
}
