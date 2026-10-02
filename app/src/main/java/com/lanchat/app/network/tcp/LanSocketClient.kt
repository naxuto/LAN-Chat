package com.lanchat.app.network.tcp

import com.lanchat.app.network.protocol.Packet
import com.lanchat.app.network.protocol.PacketSerializer
import com.lanchat.app.network.security.EncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.SecretKey

class LanSocketClient(
    private val encryptionManager: EncryptionManager
) {
    suspend fun sendMessage(
        targetIp: String,
        targetPort: Int,
        targetPublicKey: String,
        messagePacket: Packet.MessagePacket
    ): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(targetIp, targetPort), 5000)
            socket.soTimeout = 10_000

            val outputStream = DataOutputStream(socket.getOutputStream())
            val inputStream = DataInputStream(socket.getInputStream())

            // 1. Receive Auth Challenge
            val challengeRaw = readFramedPacket(inputStream) ?: return@withContext false
            val challengePacket = PacketSerializer.deserialize(challengeRaw) as? Packet.AuthChallenge ?: return@withContext false

            // 2. Respond with Identity Signature
            val signature = encryptionManager.signData(challengePacket.challenge.toByteArray(Charsets.UTF_8))
            val authResponse = Packet.AuthResponse(
                signature = signature,
                publicKey = encryptionManager.getPublicKeyBase64()
            )
            sendFramedPacket(outputStream, PacketSerializer.serialize(authResponse))

            // 3. Encrypt payload with derived session key
            val sessionKey: SecretKey = encryptionManager.deriveSharedSecret(targetPublicKey)
            val encryptedBody = encryptionManager.encryptPayload(messagePacket.encryptedBody, sessionKey)
            val securePacket = messagePacket.copy(encryptedBody = encryptedBody)

            // 4. Send Message
            sendFramedPacket(outputStream, PacketSerializer.serialize(securePacket))

            // 5. Read ACK
            val ackRaw = readFramedPacket(inputStream) ?: return@withContext false
            val ackPacket = PacketSerializer.deserialize(ackRaw) as? Packet.AckPacket ?: return@withContext false

            ackPacket.messageId == messagePacket.messageId && ackPacket.status == "DELIVERED"
        } catch (e: Exception) {
            false
        } finally {
            try { socket?.close() } catch (e: Exception) {}
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
