package com.sap.codelab.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.sap.codelab.R

private const val CHANNEL_ID = "geofence_channel"

fun sendNotification(context: Context, title: String, message: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.memo_notification_title),
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_location_reached)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}