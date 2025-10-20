package com.sap.codelab.view.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityViewMemoBinding
import com.sap.codelab.model.Memo
import kotlinx.coroutines.launch

internal const val BUNDLE_MEMO_ID: String = "memoId"

/**
 * Activity that allows a user to see the details of a memo.
 */
internal class ViewMemo : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityViewMemoBinding
    private var map: GoogleMap? = null
    private var currentMemo: Memo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize views with the passed memo id
        val model = ViewModelProvider(this)[ViewMemoViewModel::class.java]
        if (savedInstanceState == null) {
            // Observe the memo state flow for changes
            lifecycleScope.launch {
                model.memo.collect { value ->
                    value?.let { memo ->
                        currentMemo = memo
                        // Update the UI whenever the memo changes
                        updateUI(memo)
                    }
                }
            }
            val id = intent.getLongExtra(BUNDLE_MEMO_ID, -1)
            model.loadMemo(id)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isMapToolbarEnabled = false
        currentMemo?.let { showMemoLocation(it) }
    }

    /**
     * Updates the UI with the given memo details.
     *
     * @param memo - the memo whose details are to be displayed.
     */
    private fun updateUI(memo: Memo) {
        binding.contentCreateMemo.run {
            memoTitle.setText(memo.title)
            memoDescription.setText(memo.description)
            memoTitle.isEnabled = false
            memoDescription.isEnabled = false
            if (memo.reminderLatitude != null && memo.reminderLongitude != null) {
                coordinates.text = getString(
                    R.string.lat_long,
                    memo.reminderLatitude.toString(),
                    memo.reminderLongitude.toString()
                )
            } else {
                coordinates.visibility = View.GONE
            }
        }
        showMemoLocation(memo)
    }

    private fun showMemoLocation(memo: Memo) {
        map?.let { googleMap ->
            if (memo.reminderLatitude != null && memo.reminderLongitude != null) {
                val location = LatLng(memo.reminderLatitude!!, memo.reminderLongitude!!)
                googleMap.addMarker(MarkerOptions().position(location).title(memo.title))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                googleMap.uiSettings.isScrollGesturesEnabled = false
                googleMap.uiSettings.isZoomGesturesEnabled = false
            }
        }
    }
}
