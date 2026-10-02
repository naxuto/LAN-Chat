package com.lanchat.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PeerState { ONLINE, OFFLINE, CONNECTION_LOST, DISCOVERED, CONNECTING }

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val ipAddress: String,
    val port: Int,
    val lastSeen: Long,
    val state: PeerState,
    val isTrusted: Boolean = false,
    val isBlocked: Boolean = false,
    val publicKey: String
)
