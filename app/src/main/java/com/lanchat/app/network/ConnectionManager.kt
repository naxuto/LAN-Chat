package com.lanchat.app.network

import android.content.Context
import com.lanchat.app.data.entities.MessageEntity
import com.lanchat.app.data.entities.MessageStatus
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.data.entities.PeerState
import com.lanchat.app.network.discovery.PeerDiscoveryManager
import com.lanchat.app.network.protocol.Packet
import com.lanchat.app.network.tcp.LanSocketClient
import com.lanchat.app.network.tcp.LanSocketServer
import com.lanchat.app.repository.MessageRepository
import com.lanchat.app.repository.PeerRepository
import com.lanchat.app.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ConnectionManager(
    private val context: Context,
    private val userRepository: UserRepository,
    private val peerRepository: PeerRepository,
    private val messageRepository: MessageRepository,
    private val peerDiscoveryManager: PeerDiscoveryManager,
    private val lanSocketServer: LanSocketServer,
    private val lanSocketClient: LanSocketClient,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true

        lanSocketServer.startServer(scope)

        scope.launch {
            networkMonitor.networkStatus.collectLatest { status ->
                if (status.isLanAvailable) {
                    peerDiscoveryManager.startDiscovery(scope)
                } else {
                    peerDiscoveryManager.stopDiscovery()
                }
            }
        }

        // Handle discovered peers
        scope.launch {
            peerDiscoveryManager.discoveredPeers.collect { peer ->
                val entity = PeerEntity(
                    deviceId = peer.deviceId,
                    displayName = peer.deviceName,
                    ipAddress = peer.ipAddress,
                    port = peer.port,
                    lastSeen = System.currentTimeMillis(),
                    state = PeerState.ONLINE,
                    publicKey = peer.publicKey
                )
                peerRepository.upsertPeer(entity)
                flushPendingMessages(peer.deviceId)
            }
        }

        // Handle incoming messages
        scope.launch {
            lanSocketServer.incomingMessages.collect { incoming ->
                val packet = incoming.messagePacket
                // Always use OUR local conversation id for this sender
                // (the sender's conversationId is "direct_<our id>", which is wrong for us).
                val peerName = peerRepository.getPeer(packet.senderId)?.displayName
                    ?: "Peer ${packet.senderId.take(4)}"
                val conversationId = messageRepository.ensureDirectConversation(packet.senderId, peerName)

                val messageEntity = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    messageId = packet.messageId,
                    conversationId = conversationId,
                    senderId = packet.senderId,
                    receiverId = packet.receiverId,
                    body = packet.encryptedBody, // already decrypted by LanSocketServer
                    timestamp = packet.timestamp,
                    status = MessageStatus.DELIVERED
                )

                messageRepository.saveMessage(messageEntity)
            }
        }

        // Peer online heartbeat watchdog
        scope.launch {
            while (isActive) {
                delay(10000)
                val now = System.currentTimeMillis()
                val peers = peerRepository.allPeers.first()
                peers.forEach { peer ->
                    if (peer.state == PeerState.ONLINE && (now - peer.lastSeen) > 15000) {
                        peerRepository.markState(peer.deviceId, PeerState.OFFLINE)
                    }
                }
            }
        }
    }

    suspend fun sendDirectMessage(peerDeviceId: String, body: String): Boolean {
        val user = userRepository.getOrCreateUser()
        val peer = peerRepository.getPeer(peerDeviceId) ?: return false

        val conversationId = messageRepository.ensureDirectConversation(peer.deviceId, peer.displayName)
        val messageId = UUID.randomUUID().toString()

        val localEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            conversationId = conversationId,
            senderId = user.deviceId,
            receiverId = peer.deviceId,
            body = body,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING
        )

        messageRepository.saveMessage(localEntity)

        if (peer.state == PeerState.ONLINE) {
            val packet = Packet.MessagePacket(
                messageId = messageId,
                senderId = user.deviceId,
                receiverId = peer.deviceId,
                conversationId = conversationId,
                timestamp = localEntity.timestamp,
                encryptedBody = body
            )

            val success = lanSocketClient.sendMessage(
                peer.ipAddress,
                peer.port,
                peer.publicKey,
                packet
            )

            if (success) {
                messageRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                return true
            } else {
                messageRepository.updateMessageStatus(messageId, MessageStatus.PENDING)
            }
        }
        return false
    }

    suspend fun sendGroupMessage(groupId: String, body: String) {
        val members = messageRepository.getGroupMembers(groupId)
        val user = userRepository.getOrCreateUser()

        members.forEach { memberId ->
            if (memberId != user.deviceId) {
                sendDirectMessage(memberId, body)
            }
        }
    }

    private suspend fun flushPendingMessages(peerDeviceId: String) {
        val pendingList = messageRepository.getPendingMessages(peerDeviceId)
        if (pendingList.isEmpty()) return

        val peer = peerRepository.getPeer(peerDeviceId) ?: return
        val user = userRepository.getOrCreateUser()

        for (msg in pendingList) {
            val packet = Packet.MessagePacket(
                messageId = msg.messageId,
                senderId = user.deviceId,
                receiverId = peer.deviceId,
                conversationId = msg.conversationId,
                timestamp = msg.timestamp,
                encryptedBody = msg.body
            )

            val success = lanSocketClient.sendMessage(
                peer.ipAddress,
                peer.port,
                peer.publicKey,
                packet
            )

            if (success) {
                messageRepository.updateMessageStatus(msg.messageId, MessageStatus.DELIVERED)
            } else {
                break
            }
        }
    }

    fun stop() {
        started = false
        peerDiscoveryManager.stopDiscovery()
        lanSocketServer.stopServer()
    }
}
