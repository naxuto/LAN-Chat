package com.lanchat.app.network.tcp

import com.lanchat.app.network.protocol.Packet
import com.lanchat.app.network.protocol.PacketSerializer
import com.lanchat.app.network.security.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

data class IncomingMessageEvent(
    val messagePacket: Packet.MessagePacket,
    val senderIp: String
)

class LanSocketServer(
    private val encryptionManager: EncryptionManager
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var listenJob: Job? = null

    private val _incomingMessages = MutableSharedFlow<IncomingMessageEvent>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<IncomingMessageEvent> = _incomingMessages

    fun startServer(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(Packet.TCP_PORT)
                while (isActive && isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopServer() {
        isRunning = false
        listenJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }

    private suspend fun handleClientConnection(socket: Socket) {
        val senderIp = socket.inetAddress.hostAddress ?: ""
        try {
            socket.soTimeout = 10_000
            val inputStream = DataInputStream(socket.getInputStream())
            val outputStream = DataOutputStream(socket.getOutputStream())

            // 1. Challenge-Response Auth & Handshake
            val challenge = UUID.randomUUID().toString()
            val challengePacket = Packet.AuthChallenge(challenge)
            sendFramedPacket(outputStream, PacketSerializer.serialize(challengePacket))

            val authRespRaw = readFramedPacket(inputStream) ?: return
            val authResp = PacketSerializer.deserialize(authRespRaw) as? Packet.AuthResponse ?: return

            val isValid = encryptionManager.verifySignature(
                challenge.toByteArray(Charsets.UTF_8),
                authResp.signature,
                authResp.publicKey
            )
            if (!isValid) {
                socket.close()
                return
            }

            val sessionKey = encryptionManager.deriveSharedSecret(authResp.publicKey)

            // 2. Read incoming encrypted message payload
            val msgPacketRaw = readFramedPacket(inputStream) ?: return
            val msgPacket = PacketSerializer.deserialize(msgPacketRaw) as? Packet.MessagePacket ?: return

            // Decrypt message body
            val decryptedBody = encryptionManager.decryptPayload(msgPacket.encryptedBody, sessionKey)
            val fullMsg = msgPacket.copy(encryptedBody = decryptedBody)

            _incomingMessages.emit(IncomingMessageEvent(fullMsg, senderIp))

            // 3. Send ACK back
            val ack = Packet.AckPacket(
                messageId = msgPacket.messageId,
                senderId = msgPacket.receiverId,
                status = "DELIVERED"
            )
            sendFramedPacket(outputStream, PacketSerializer.serialize(ack))

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun sendFramedPacket(outputStream: DataOutputStream, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        outputStream.writeInt(bytes.size)
        outputStream.write(bytes)
        outputStream.flush()
    }

    private fun readFramedPacket(inputStream: DataInputStream): String? {
        val length = inputStream.readInt()
        if (length <= 0 || length > Packet.MAX_PACKET_SIZE) return null
        val buffer = ByteArray(length)
        inputStream.readFully(buffer)
        return String(buffer, Charsets.UTF_8)
    }
}
