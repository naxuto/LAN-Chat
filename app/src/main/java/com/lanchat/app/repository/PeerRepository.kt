package com.lanchat.app.repository

import com.lanchat.app.data.dao.PeerDao
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.data.entities.PeerState
import kotlinx.coroutines.flow.Flow

class PeerRepository(private val peerDao: PeerDao) {
    val allPeers: Flow<List<PeerEntity>> = peerDao.getAllPeers()
    val trustedPeers: Flow<List<PeerEntity>> = peerDao.getTrustedPeers()

    suspend fun upsertPeer(peer: PeerEntity) {
        val existing = peerDao.getPeerById(peer.deviceId)
        if (existing != null) {
            peerDao.insertOrUpdate(
                peer.copy(
                    isTrusted = existing.isTrusted,
                    isBlocked = existing.isBlocked
                )
            )
        } else {
            peerDao.insertOrUpdate(peer)
        }
    }

    suspend fun getPeer(deviceId: String): PeerEntity? = peerDao.getPeerById(deviceId)

    suspend fun updateState(deviceId: String, state: PeerState) {
        peerDao.updateState(deviceId, state)
    }

    /** Changes state without touching lastSeen (used by the offline watchdog). */
    suspend fun markState(deviceId: String, state: PeerState) {
        peerDao.updateStateOnly(deviceId, state)
    }

    suspend fun setTrusted(deviceId: String, isTrusted: Boolean) {
        peerDao.updateTrustStatus(deviceId, isTrusted)
    }

    suspend fun setBlocked(deviceId: String, isBlocked: Boolean) {
        peerDao.updateBlockStatus(deviceId, isBlocked)
    }

    suspend fun removePeer(deviceId: String) {
        peerDao.deletePeer(deviceId)
    }
}
