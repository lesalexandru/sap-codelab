package com.sap.codelab.repository

import android.content.Context
import android.location.Location
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.sap.codelab.presentation.receiver.GeofenceManager
import com.sap.codelab.data.location.LocationManager
import com.sap.codelab.model.Memo
import com.sap.codelab.presentation.notification.sendNotification
import com.sap.codelab.utils.coroutines.ScopeProvider
import kotlinx.coroutines.launch

private const val DATABASE_NAME: String = "codelab"
private const val NOTIFICATION_MESSAGE_SIZE = 140

/**
 * The repository is used to retrieve data from a data source.
 */
internal object Repository : IMemoRepository {

    private lateinit var database: Database
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var locationManager: LocationManager
    private lateinit var applicationContext: Context

    fun initialize(applicationContext: Context) {
        this.applicationContext = applicationContext
        database = Room.databaseBuilder(applicationContext, Database::class.java, DATABASE_NAME).build()
        geofenceManager = GeofenceManager(applicationContext)
        locationManager = LocationManager(applicationContext)
    }

    @WorkerThread
    override fun saveMemo(memo: Memo) {
        locationManager.getCurrentLocation()?.addOnSuccessListener { location ->
            ScopeProvider.application.launch {
                val newId = database.getMemoDao().insert(memo)
                var savedMemo = memo.copy(id = newId)
                if (!savedMemo.isDone) {
                    if (location != null && savedMemo.reminderLatitude != null && savedMemo.reminderLongitude != null) {
                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude,
                            location.longitude,
                            savedMemo.reminderLatitude!!,
                            savedMemo.reminderLongitude!!,
                            distance
                        )

                        if (distance[0] < 200) {
                            savedMemo = savedMemo.copy(isDone = true)
                            database.getMemoDao().insert(savedMemo)
                            sendNotification(
                                context = applicationContext,
                                title = savedMemo.title,
                                message = savedMemo.description.take(NOTIFICATION_MESSAGE_SIZE),
                            )
                        } else {
                            addGeofence(savedMemo)
                        }
                    } else {
                        addGeofence(savedMemo)
                    }
                } else {
                    removeGeofence(savedMemo)
                }
            }
        } ?: ScopeProvider.application.launch {
            val newId = database.getMemoDao().insert(memo)
            val savedMemo = memo.copy(id = newId)
            if (!savedMemo.isDone) {
                addGeofence(savedMemo)
            } else {
                removeGeofence(savedMemo)
            }
        }
    }

    @WorkerThread
    override fun getOpen(): List<Memo> = database.getMemoDao().getOpen()

    @WorkerThread
    override fun getAll(): List<Memo> = database.getMemoDao().getAll()

    @WorkerThread
    override fun getMemoById(id: Long): Memo = database.getMemoDao().getMemoById(id)

    @WorkerThread
    override fun addGeofence(memo: Memo) {
        geofenceManager.addGeofence(memo)
    }

    @WorkerThread
    override fun removeGeofence(memo: Memo) {
        geofenceManager.removeGeofence(memo)
    }
}