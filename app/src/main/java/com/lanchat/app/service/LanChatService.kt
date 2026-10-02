package com.lanchat.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lanchat.app.LanChatApplication

class LanChatService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as LanChatApplication
        startForegroundServiceInternal()
        app.connectionManager.start()
    }

    private fun startForegroundServiceInternal() {
        val notification: Notification = NotificationCompat.Builder(this, LanChatApplication.SERVICE_CHANNEL_ID)
            .setContentTitle("LAN Chat Active")
            .setContentText("Listening for peer messages on local network...")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        val app = application as LanChatApplication
        app.connectionManager.stop()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
