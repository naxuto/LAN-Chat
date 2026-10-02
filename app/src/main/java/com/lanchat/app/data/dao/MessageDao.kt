package com.lanchat.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lanchat.app.data.entities.MessageEntity
import com.lanchat.app.data.entities.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' AND receiverId = :receiverId")
    suspend fun getPendingMessagesForReceiver(receiverId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE messageId = :messageId)")
    suspend fun exists(messageId: String): Boolean
}
