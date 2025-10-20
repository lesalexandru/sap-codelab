package com.sap.codelab.view.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityCreateMemoBinding
import com.sap.codelab.utils.extensions.empty

private const val PACKAGE_SCHEME = "package"

/**
 * Activity that allows a user to create a new Memo.
 */
internal class CreateMemo : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCreateMemoBinding
    private lateinit var model: CreateMemoViewModel
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLocation: LatLng? = null
    private lateinit var defaultCoordinatesTextColor: ColorStateList

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeMap()
        } else {
            handleLocationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        model = ViewModelProvider(this)[CreateMemoViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        defaultCoordinatesTextColor = binding.contentCreateMemo.coordinates.textColors

        checkAndRequestLocationPermission()
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeMap()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun handleLocationPermissionDenied() {
        binding.contentCreateMemo.coordinates.text = getString(R.string.location_permission_required)
        binding.contentCreateMemo.coordinates.setTextColor(Color.RED)
        binding.contentCreateMemo.coordinates.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts(PACKAGE_SCHEME, packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.view?.alpha = 0.5f
        mapFragment?.view?.isClickable = false
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Reset the coordinates view to its default state in case it was showing a permission error
        binding.contentCreateMemo.coordinates.text = getString(R.string.location_select_prompt)
        binding.contentCreateMemo.coordinates.setTextColor(defaultCoordinatesTextColor)
        binding.contentCreateMemo.coordinates.setOnClickListener(null)

        // Double-check permission before accessing location-sensitive APIs
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }

        map.setOnMapLongClickListener { latLng ->
            selectedLocation = latLng
            binding.contentCreateMemo.coordinates.text = getString(
                R.string.lat_long,
                latLng.latitude.toString(),
                latLng.longitude.toString()
            )
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title(getString(R.string.selected_location)))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_memo, menu)
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveMemo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Saves the memo if the input is valid; otherwise shows the corresponding error messages.
     */
    private fun saveMemo() {
        binding.contentCreateMemo.run {
            model.updateMemo(
                title = memoTitle.text.toString(),
                description = memoDescription.text.toString(),
                latitude = selectedLocation?.latitude,
                longitude = selectedLocation?.longitude,
            )
            if (model.isMemoValid()) {
                model.saveMemo()
                setResult(RESULT_OK)
                finish()
            } else {
                memoTitleContainer.error = getErrorMessage(model.hasTitleError(), R.string.memo_title_empty_error)
                memoDescription.error = getErrorMessage(model.hasTextError(), R.string.memo_text_empty_error)
            }
        }
    }

    /**
     * Returns the error message if there is an error, or an empty string otherwise.
     *
     * @param hasError          - whether there is an error.
     * @param errorMessageResId - the resource id of the error message to show.
     * @return the error message if there is an error, or an empty string otherwise.
     */
    private fun getErrorMessage(hasError: Boolean, @StringRes errorMessageResId: Int): String {
        return if (hasError) {
            getString(errorMessageResId)
        } else {
            String.empty()
        }
    }
}
