package com.lanchat.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.data.entities.PeerState
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers WHERE isBlocked = 0 ORDER BY lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE isTrusted = 1 AND isBlocked = 0")
    fun getTrustedPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPeerById(deviceId: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(peer: PeerEntity)

    @Query("UPDATE peers SET state = :state, lastSeen = :lastSeen WHERE deviceId = :deviceId")
    suspend fun updateState(deviceId: String, state: PeerState, lastSeen: Long = System.currentTimeMillis())

    @Query("UPDATE peers SET state = :state WHERE deviceId = :deviceId")
    suspend fun updateStateOnly(deviceId: String, state: PeerState)

    @Query("UPDATE peers SET isTrusted = :isTrusted WHERE deviceId = :deviceId")
    suspend fun updateTrustStatus(deviceId: String, isTrusted: Boolean)

    @Query("UPDATE peers SET isBlocked = :isBlocked WHERE deviceId = :deviceId")
    suspend fun updateBlockStatus(deviceId: String, isBlocked: Boolean)

    @Query("DELETE FROM peers WHERE deviceId = :deviceId")
    suspend fun deletePeer(deviceId: String)
}
