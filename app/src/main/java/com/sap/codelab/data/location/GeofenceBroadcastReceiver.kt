package com.sap.codelab.data.location

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.sap.codelab.presentation.notification.sendNotification
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.coroutines.ScopeProvider
import kotlinx.coroutines.launch

private const val TAG = "GeofenceReceiver"
private const val NOTIFICATION_MESSAGE_SIZE = 140

class GeofenceBroadcastReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = "Geofence Error: " + geofencingEvent.errorCode
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            geofencingEvent.triggeringGeofences?.forEach { geofence ->
                val memoId = geofence.requestId.toLong()
                ScopeProvider.application.launch {
                    val memo = Repository.getMemoById(memoId)
                    if (!memo.isDone) {
                        val updatedMemo = memo.copy(isDone = true)
                        Repository.saveMemo(updatedMemo)
                        Repository.removeGeofence(updatedMemo)
                        sendNotification(
                            context = context,
                            title = memo.title,
                            message = memo.description.take(NOTIFICATION_MESSAGE_SIZE),
                        )
                    }
                }
            }
        }
    }
}