package com.sap.codelab.presentation.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sap.codelab.data.location.GeofenceBroadcastReceiver
import com.sap.codelab.model.Memo

private const val GEOFENCE_RADIUS_IN_METERS = 200f

internal class GeofenceManager(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGeofence(memo: Memo) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            memo.reminderLatitude?.let { latitude ->
                memo.reminderLongitude?.let { longitude ->
                    val geofence = Geofence.Builder()
                        .setRequestId(memo.id.toString())
                        .setCircularRegion(
                            latitude,
                            longitude,
                            GEOFENCE_RADIUS_IN_METERS
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()

                    val geofencingRequest = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofence(geofence)
                        .build()

                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                }
            }
        }
    }

    fun removeGeofence(memo: Memo) {
        geofencingClient.removeGeofences(listOf(memo.id.toString()))
    }
}
