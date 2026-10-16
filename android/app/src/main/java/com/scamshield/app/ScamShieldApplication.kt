package com.scamshield.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.scamshield.app.network.SocketManager

class ScamShieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        
        // Initialize Socket.io connection for real-time risk updates
        SocketManager.init(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROTECTION,
                "Scam Shield Active Defense",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows real-time status of call, chat, and banking overlay protections"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_PROTECTION = "scam_shield_protection_channel"
        lateinit var instance: ScamShieldApplication
            private set
    }
}
