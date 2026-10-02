package com.lanchat.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lanchat.app.data.dao.ConversationDao
import com.lanchat.app.data.dao.MessageDao
import com.lanchat.app.data.dao.PeerDao
import com.lanchat.app.data.dao.UserDao
import com.lanchat.app.data.entities.ConversationEntity
import com.lanchat.app.data.entities.ConversationMemberEntity
import com.lanchat.app.data.entities.MessageEntity
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.data.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PeerEntity::class,
        ConversationEntity::class,
        ConversationMemberEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LanChatDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun peerDao(): PeerDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: LanChatDatabase? = null

        fun getInstance(context: Context): LanChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LanChatDatabase::class.java,
                    "lan_chat_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
