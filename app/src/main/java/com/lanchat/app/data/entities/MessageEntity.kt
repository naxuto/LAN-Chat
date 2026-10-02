package com.lanchat.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageStatus { PENDING, SENDING, SENT, DELIVERED, FAILED, READ }

@Entity(
    tableName = "messages",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String, // May be peer device ID or group conversation ID
    val body: String,
    val timestamp: Long,
    val status: MessageStatus
)
