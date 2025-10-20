package com.sap.codelab.repository

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

private const val CHANNEL_ID = "memo_reminder_channel"
private const val CHANNEL_NAME = "Memo Location Area"
private const val CHANNEL_DESCRIPTION = "Notification for memos that are within 200 meters of current location."

/**
 * Extension of the Android Application class.
 */
internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = CHANNEL_NAME
        val descriptionText = CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}