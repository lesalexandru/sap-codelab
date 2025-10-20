package com.sap.codelab.presentation.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.sap.codelab.R
import com.sap.codelab.utils.PermissionUtils

private const val PACKAGE_SCHEME = "package"

class PermissionHelper(private val activity: ComponentActivity) {

    private val requestNotificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* NOOP */ }

    private val requestBackgroundLocationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* NOOP */ }

    private val requestLocationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                requestBackgroundLocationPermission()
            }
        }

    fun requestLocationPermission() {
        when {
            PermissionUtils.hasFineLocationPermission(activity) -> {
                requestBackgroundLocationPermission()
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionUtils.hasBackgroundLocationPermission(activity)) {
                AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.background_location_required))
                    .setMessage(activity.getString(R.string.bg_location_required_message))
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(PACKAGE_SCHEME, activity.packageName, null)
                        intent.data = uri
                        activity.startActivity(intent)
                    }
                    .setNegativeButton(activity.getString(R.string.cancel), null)
                    .create()
                    .show()
            }
        }
    }

    fun requestNotificationPermission() {
        if (shouldShowNotificationRationale()) {
            showNotificationRationale()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun shouldShowNotificationRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }

    private fun showNotificationRationale() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_rationale_title)
            .setMessage(R.string.notification_permission_rationale_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
