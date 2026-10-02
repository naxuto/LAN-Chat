package com.lanchat.app.repository

import com.lanchat.app.data.dao.ConversationDao
import com.lanchat.app.data.dao.MessageDao
import com.lanchat.app.data.entities.ConversationEntity
import com.lanchat.app.data.entities.ConversationMemberEntity
import com.lanchat.app.data.entities.MessageEntity
import com.lanchat.app.data.entities.MessageStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun ensureDirectConversation(peerDeviceId: String, peerName: String): String {
        val conversationId = "direct_$peerDeviceId"
        conversationDao.insertConversation(
            ConversationEntity(
                conversationId = conversationId,
                title = peerName,
                isGroup = false
            )
        )
        conversationDao.insertMember(ConversationMemberEntity(conversationId, peerDeviceId))
        return conversationId
    }

    suspend fun createGroupConversation(title: String, memberDeviceIds: List<String>): String {
        val groupId = "group_" + UUID.randomUUID().toString()
        conversationDao.insertConversation(
            ConversationEntity(
                conversationId = groupId,
                title = title,
                isGroup = true
            )
        )
        memberDeviceIds.forEach { deviceId ->
            conversationDao.insertMember(ConversationMemberEntity(groupId, deviceId))
        }
        return groupId
    }

    suspend fun saveMessage(message: MessageEntity): Boolean {
        val inserted = messageDao.insertMessage(message)
        return inserted != -1L
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateStatus(messageId, status)
    }

    suspend fun getPendingMessages(receiverId: String): List<MessageEntity> {
        return messageDao.getPendingMessagesForReceiver(receiverId)
    }

    suspend fun getGroupMembers(groupId: String): List<String> {
        return conversationDao.getMembers(groupId)
    }
}
