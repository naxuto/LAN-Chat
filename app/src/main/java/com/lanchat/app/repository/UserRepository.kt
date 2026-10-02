package com.lanchat.app.repository

import android.content.Context
import android.os.Build
import com.lanchat.app.data.dao.UserDao
import com.lanchat.app.data.entities.UserEntity
import com.lanchat.app.network.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val encryptionManager: EncryptionManager
) {
    val userFlow: Flow<UserEntity?> = userDao.getUser()

    suspend fun getOrCreateUser(): UserEntity {
        val existing = userDao.getUserSync()
        if (existing != null) return existing

        val prefs = context.getSharedPreferences("lan_chat_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }

        val defaultName = "Android (${Build.MODEL})"
        val user = UserEntity(
            deviceId = deviceId,
            displayName = defaultName,
            publicKey = encryptionManager.getPublicKeyBase64()
        )
        userDao.insertOrUpdate(user)
        return user
    }

    suspend fun updateDisplayName(name: String) {
        val currentUser = getOrCreateUser()
        userDao.insertOrUpdate(currentUser.copy(displayName = name))
    }
}
