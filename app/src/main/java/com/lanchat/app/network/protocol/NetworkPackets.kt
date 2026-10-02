package com.lanchat.app.network.protocol

import org.json.JSONObject

sealed class Packet {
    abstract val type: String

    data class Discovery(
        val protocol: String = PROTOCOL_NAME,
        val version: Int = PROTOCOL_VERSION,
        val deviceId: String
    ) : Packet() {
        override val type = "DISCOVER"
    }

    data class DiscoveryResponse(
        val protocol: String = PROTOCOL_NAME,
        val version: Int = PROTOCOL_VERSION,
        val deviceId: String,
        val deviceName: String,
        val port: Int,
        val publicKey: String
    ) : Packet() {
        override val type = "DISCOVER_RESPONSE"
    }

    data class AuthChallenge(
        val challenge: String
    ) : Packet() {
        override val type = "AUTH_CHALLENGE"
    }

    data class AuthResponse(
        val signature: String,
        val publicKey: String
    ) : Packet() {
        override val type = "AUTH_RESPONSE"
    }

    data class MessagePacket(
        val messageId: String,
        val senderId: String,
        val receiverId: String,
        val conversationId: String,
        val timestamp: Long,
        val encryptedBody: String
    ) : Packet() {
        override val type = "MESSAGE"
    }

    data class AckPacket(
        val messageId: String,
        val senderId: String,
        val status: String
    ) : Packet() {
        override val type = "ACK"
    }

    companion object {
        const val PROTOCOL_NAME = "LAN_CHAT"
        const val PROTOCOL_VERSION = 1
        const val UDP_PORT = 45679
        const val TCP_PORT = 45678
        const val MAX_PACKET_SIZE = 128 * 1024 // 128 KB
    }
}

object PacketSerializer {

    fun serialize(packet: Packet): String {
        val json = JSONObject()
        json.put("type", packet.type)
        when (packet) {
            is Packet.Discovery -> {
                json.put("protocol", packet.protocol)
                json.put("version", packet.version)
                json.put("deviceId", packet.deviceId)
            }
            is Packet.DiscoveryResponse -> {
                json.put("protocol", packet.protocol)
                json.put("version", packet.version)
                json.put("deviceId", packet.deviceId)
                json.put("deviceName", packet.deviceName)
                json.put("port", packet.port)
                json.put("publicKey", packet.publicKey)
            }
            is Packet.AuthChallenge -> {
                json.put("challenge", packet.challenge)
            }
            is Packet.AuthResponse -> {
                json.put("signature", packet.signature)
                json.put("publicKey", packet.publicKey)
            }
            is Packet.MessagePacket -> {
                json.put("messageId", packet.messageId)
                json.put("senderId", packet.senderId)
                json.put("receiverId", packet.receiverId)
                json.put("conversationId", packet.conversationId)
                json.put("timestamp", packet.timestamp)
                json.put("encryptedBody", packet.encryptedBody)
            }
            is Packet.AckPacket -> {
                json.put("messageId", packet.messageId)
                json.put("senderId", packet.senderId)
                json.put("status", packet.status)
            }
        }
        return json.toString()
    }

    fun deserialize(raw: String): Packet? {
        return try {
            val json = JSONObject(raw)
            when (json.getString("type")) {
                "DISCOVER" -> Packet.Discovery(
                    protocol = json.optString("protocol", Packet.PROTOCOL_NAME),
                    version = json.optInt("version", Packet.PROTOCOL_VERSION),
                    deviceId = json.getString("deviceId")
                )
                "DISCOVER_RESPONSE" -> Packet.DiscoveryResponse(
                    protocol = json.optString("protocol", Packet.PROTOCOL_NAME),
                    version = json.optInt("version", Packet.PROTOCOL_VERSION),
                    deviceId = json.getString("deviceId"),
                    deviceName = json.getString("deviceName"),
                    port = json.getInt("port"),
                    publicKey = json.getString("publicKey")
                )
                "AUTH_CHALLENGE" -> Packet.AuthChallenge(
                    challenge = json.getString("challenge")
                )
                "AUTH_RESPONSE" -> Packet.AuthResponse(
                    signature = json.getString("signature"),
                    publicKey = json.getString("publicKey")
                )
                "MESSAGE" -> Packet.MessagePacket(
                    messageId = json.getString("messageId"),
                    senderId = json.getString("senderId"),
                    receiverId = json.getString("receiverId"),
                    conversationId = json.getString("conversationId"),
                    timestamp = json.getLong("timestamp"),
                    encryptedBody = json.getString("encryptedBody")
                )
                "ACK" -> Packet.AckPacket(
                    messageId = json.getString("messageId"),
                    senderId = json.getString("senderId"),
                    status = json.getString("status")
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
