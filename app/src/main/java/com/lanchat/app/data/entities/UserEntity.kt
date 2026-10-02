package com.lanchat.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val publicKey: String,
    val createdAt: Long = System.currentTimeMillis()
)
