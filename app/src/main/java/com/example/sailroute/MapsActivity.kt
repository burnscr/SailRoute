
package com.example.sailroute

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.Html
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.example.sailroute.callbacks.MarkerHandlers

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnPolylineClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

import com.example.sailroute.databinding.ActivityMapsBinding
import com.example.sailroute.sailing.Pathfinding
import com.example.sailroute.sailing.Route
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.sailroute.util.*
import com.google.android.gms.maps.model.*
import java.text.DecimalFormat

private fun String.toDegOrNull(): Int? {
    val x = this.toIntOrNull() ?: return null
    return x.bindTo360()
}

private fun Double.toNumberString(): String {
    return if (abs(this - this.roundToInt()) < 0.0001) {
        this.roundToInt().toString()
    } else {
        this.toString()
    }
}

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMapLongClickListener, OnPolylineClickListener {

    private var _map: GoogleMap? = null
    private val map get() = _map!!
    private lateinit var binding: ActivityMapsBinding
    private val viewModel: MapsViewModel by viewModels()

    private lateinit var routing: MapsRouting
    private var marker: Marker? = null

    // Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val requestPermissionLauncher: ActivityResultLauncher<String>

    init {
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.requestingLocationUpdates.set(true)
                    startLocationUpdates()
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    AlertDialog.Builder(this)
                        .setTitle("Location Services")
                        .setMessage("This app relies on location services to provide accurate " +
                                "navigation instructions. Without the location permission, routes " +
                                "cannot be determined.")
                        .setPositiveButton("Ok") { _, _ -> }
                        .setIcon(R.drawable.gps_off)
                        .show()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup binding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Process location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                viewModel.lastKnownLocation.value = locationResult.lastLocation
                updateRoute()
            }
        }

        viewModel.currentRoute.observe(this, this::updateInstructions)
        viewModel.selectedDestination.observe(this) { updateRoute() }

        setupEditTextObservers()
        setupEditWindObservers()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.requestingLocationUpdates.get()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        _map = null
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        _map = googleMap
        routing = MapsRouting(map)

        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))

        map.setPadding(0, 625, 0, 300)
        map.setOnMapLongClickListener(this)
        map.setOnMarkerDragListener(MarkerHandlers(viewModel))
        map.setOnPolylineClickListener(this)
        map.uiSettings.isMapToolbarEnabled = false

        viewModel.selectedDestination.value?.let { destination ->
            onMapLongClick(destination.toLatLng())
        }

        onlyWithPermission {
            startLocationUpdates()
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        marker?.let {
            it.position = latLng
            it.snippet = "${latLng.getLatitudeDegrees()} ${latLng.getLongitudeDegrees()}"
        } ?: run {
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Destination")
                    .snippet("${latLng.getLatitudeDegrees()} ${latLng.getLongitudeDegrees()}"))
        }
        marker?.showInfoWindow()
        viewModel.selectedDestination.value = latLng.toLocation()
        updateRoute()
    }

    override fun onPolylineClick(polyline: Polyline) {
        val location = viewModel.lastKnownLocation.value ?: return
        val routes = viewModel.lastRoutes.value ?: return
        val primarySelected = viewModel.primaryIsSelected.value ?: return

        viewModel.primaryIsSelected.value = !primarySelected
        routing.setOrUpdateRoute(routes, location, !primarySelected)

        // if the primary is now selected
        if (viewModel.primaryIsSelected.value!!) {
            viewModel.currentRoute.value = when {
                routes.isNotEmpty() -> routes[0]
                else -> null
            }
        }

        // if the secondary is now selected
        else {
            viewModel.currentRoute.value = when {
                routes.size > 1 -> routes[1]
                routes.isNotEmpty() -> routes[0]
                else -> null
            }
        }

        if (viewModel.currentRoute.value != null)
            updateInstructions(viewModel.currentRoute.value!!)
    }

    private fun setupEditWindObservers() {
        viewModel.windVelocity.observe(this) { updateRoute() }
        viewModel.windHeading.observe(this) { updateRoute() }
    }

    private fun updateRoute() {
        if (!this::routing.isInitialized) return
        val location = viewModel.lastKnownLocation.value ?: return
        val destination = viewModel.selectedDestination.value ?: return
        val windHeading = viewModel.windHeading.value ?: return
        val windVelocity = viewModel.windVelocity.value ?: return
        val primarySelected = viewModel.primaryIsSelected.value ?: return

        val distance = location.distanceTo(destination).toDouble()
        val bearing = (location.bearingTo(destination).roundToInt()).bindTo360()
        val routes = Pathfinding.optimalRoutes(bearing, distance, windHeading, windVelocity)
        viewModel.lastRoutes.value = routes

        // update the path drawn onto the map
        routing.setOrUpdateRoute(routes, location, primarySelected)

        // if the primary is now selected
        if (primarySelected) {
            viewModel.currentRoute.value = when {
                routes.isNotEmpty() -> routes[0]
                else -> null
            }
        }

        // if the secondary is now selected
        else {
            viewModel.currentRoute.value = when {
                routes.size > 1 -> routes[1]
                routes.isNotEmpty() -> routes[0]
                else -> null
            }
        }
    }

    private fun updateInstructions(route: Route?) {
        route ?: return
        val eta = secondsToDurationString(route.duration)
        val etaTextHtml = "<b><font color='#2e7d32'>$eta</font>  (${metersToDistanceString(route.totalDistance())})<b>"
        val html2 = Html.fromHtml(etaTextHtml, Html.FROM_HTML_MODE_COMPACT)
        binding.bottomSheet.etaText.setText(html2, TextView.BufferType.SPANNABLE)
    }

    private fun metersToDistanceString(meters: Double): String {
        val miles = meters / 1609
        val feet = meters * 3.281
        return when {
            miles >= 100 -> "${miles.toInt()} mi"
            miles >= 0.1 -> "${DecimalFormat("#.#").format(miles)} mi"
            feet > 0 -> "${feet.roundToInt()} ft"
            else -> "0 ft"
        }
    }

    private fun secondsToDurationString(seconds: Double): String {
        val secondsPerDay = 86400
        val secondsPerHour = 3600
        val secondsPerMinute = 60
        var remaining = seconds.roundToInt()

        val days = remaining / secondsPerDay
        remaining %= secondsPerDay
        val hours = remaining / secondsPerHour
        remaining %= secondsPerHour
        val minutes = remaining / secondsPerMinute

        return when {
            days > 0 && hours > 0 -> "$days d $hours hr"
            days > 0 -> "$days d"
            hours > 0 && minutes > 0 -> "$hours hr $minutes min"
            hours > 0 -> "$hours hr"
            minutes > 0 -> "$minutes min"
            else -> "1 min"
        }
    }

    // Markers

    private fun setupEditTextObservers() {

        val toHeadingString: Int?.()->String = { this?.let{"$it°"}?:"" }
        val toVelocityString: Double?.()->String = { this?.let(Double::toNumberString)?:"" }
        val toHeadingInt: String.()->Int? = String::toDegOrNull
        val toVelocityDouble: String.()->Double? = String::toDoubleOrNull

        binding.bottomSheet.let { bs ->

            // set the initial text values
            bs.windHeadingEdit.setText(viewModel.windHeading.value.toHeadingString())
            bs.windVelocityEdit.setText(viewModel.windVelocity.value.toVelocityString())

            // propagate values upstream
            bs.windHeadingEdit.setOnTypingFinished { textView ->
                val value = textView.text.toString().toHeadingInt()
                if (value == null && textView.text.isNotBlank())
                    return@setOnTypingFinished
                if (viewModel.windHeading.value != value)
                    viewModel.windHeading.value = value
                closeKeyboard(textView)
                textView.clearFocus()
            }
            bs.windVelocityEdit.setOnTypingFinished { textView ->
                val value = textView.text.toString().toVelocityDouble()
                if (value == null && textView.text.isNotBlank())
                    return@setOnTypingFinished
                if (viewModel.windVelocity.value != value)
                    viewModel.windVelocity.value = value
                closeKeyboard(textView)
                textView.clearFocus()
            }

            // propagate values downstream
            viewModel.windHeading.observe(this@MapsActivity) { value ->
                bs.windHeadingEdit.setText(value.toHeadingString()) }
            viewModel.windVelocity.observe(this@MapsActivity) { value ->
                bs.windVelocityEdit.setText(value.toVelocityString()) }
        }
    }

    private fun EditText.setOnTypingFinished(callback: (textView: TextView) -> Unit) {
        this.setOnEditorActionListener { textView, actionId, _ ->
            return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                callback(textView)
                true
            } else {
                false
            }
        }
        this.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                callback(view as TextView)
            }
        }
    }

    private fun closeKeyboard(textView: TextView) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(textView.windowToken, 0)
    }

    // Location

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 20000  // refresh every 20 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        onlyWithPermission {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            if (_map != null) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        map.animateCamera(CameraUpdateFactory.newLatLng(it.toLatLng()))
                    }
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Permissions

    private fun onlyWithPermission(callback: () -> Unit) {
        when {
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                if (!viewModel.requestingLocationUpdates.get())
                    viewModel.requestingLocationUpdates.set(true)
                callback()
            }
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Services")
                    .setMessage("This app relies on location services to provide accurate " +
                            "navigation instructions. Without the location permission, routes " +
                            "cannot be determined.")
                    .setPositiveButton("Ok") { _, _ -> }
                    .setIcon(R.drawable.gps_off)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}
