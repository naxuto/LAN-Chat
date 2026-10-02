package com.lanchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lanchat.app.data.database.LanChatDatabase
import com.lanchat.app.network.ConnectionManager
import com.lanchat.app.network.NetworkMonitor
import com.lanchat.app.network.discovery.PeerDiscoveryManager
import com.lanchat.app.network.security.EncryptionManager
import com.lanchat.app.network.tcp.LanSocketClient
import com.lanchat.app.network.tcp.LanSocketServer
import com.lanchat.app.repository.MessageRepository
import com.lanchat.app.repository.PeerRepository
import com.lanchat.app.repository.UserRepository

class LanChatApplication : Application() {

    lateinit var database: LanChatDatabase
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var peerRepository: PeerRepository
        private set

    lateinit var messageRepository: MessageRepository
        private set

    lateinit var encryptionManager: EncryptionManager
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var peerDiscoveryManager: PeerDiscoveryManager
        private set

    lateinit var lanSocketServer: LanSocketServer
        private set

    lateinit var lanSocketClient: LanSocketClient
        private set

    lateinit var connectionManager: ConnectionManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = LanChatDatabase.getInstance(this)
        encryptionManager = EncryptionManager(this)

        userRepository = UserRepository(this, database.userDao(), encryptionManager)
        peerRepository = PeerRepository(database.peerDao())
        messageRepository = MessageRepository(database.messageDao(), database.conversationDao())

        networkMonitor = NetworkMonitor(this)
        peerDiscoveryManager = PeerDiscoveryManager(this, userRepository)
        lanSocketServer = LanSocketServer(encryptionManager)
        lanSocketClient = LanSocketClient(encryptionManager)

        connectionManager = ConnectionManager(
            this,
            userRepository,
            peerRepository,
            messageRepository,
            peerDiscoveryManager,
            lanSocketServer,
            lanSocketClient,
            networkMonitor
        )

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "LAN Chat Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps LAN Chat discovery and socket server active."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "lan_chat_service_channel"
    }
}
