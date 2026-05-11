package com.scimsoft.tap2share

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TapShareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transferChannel = NotificationChannel(
                CHANNEL_TRANSFER,
                "File Transfer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during file transfers"
            }

            val receivedChannel = NotificationChannel(
                CHANNEL_RECEIVED,
                "Received Files",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when files are received"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(transferChannel)
            manager.createNotificationChannel(receivedChannel)
        }
    }

    companion object {
        const val CHANNEL_TRANSFER = "transfer_channel"
        const val CHANNEL_RECEIVED = "received_channel"
    }
}
