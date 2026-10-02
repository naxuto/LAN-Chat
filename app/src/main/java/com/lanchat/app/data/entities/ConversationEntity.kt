package com.lanchat.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val title: String,
    val isGroup: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
