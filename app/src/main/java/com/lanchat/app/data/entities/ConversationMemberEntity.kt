package com.lanchat.app.data.entities

import androidx.room.Entity

@Entity(
    tableName = "conversation_members",
    primaryKeys = ["conversationId", "deviceId"]
)
data class ConversationMemberEntity(
    val conversationId: String,
    val deviceId: String
)
