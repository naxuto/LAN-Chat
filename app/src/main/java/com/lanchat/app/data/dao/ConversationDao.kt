package com.lanchat.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lanchat.app.data.entities.ConversationEntity
import com.lanchat.app.data.entities.ConversationMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastUpdated DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: ConversationMemberEntity)

    @Query("SELECT deviceId FROM conversation_members WHERE conversationId = :conversationId")
    suspend fun getMembers(conversationId: String): List<String>
}
