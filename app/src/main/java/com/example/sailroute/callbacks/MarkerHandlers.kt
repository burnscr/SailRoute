package com.example.sailroute.callbacks

import com.example.sailroute.MapsViewModel
import com.example.sailroute.util.getLatitudeDegrees
import com.example.sailroute.util.getLongitudeDegrees
import com.example.sailroute.util.toLocation
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class MarkerHandlers(private val viewModel: MapsViewModel): GoogleMap.OnMarkerDragListener {

    override fun onMarkerDragStart(p0: Marker) {
        // Ignore
    }

    override fun onMarkerDrag(p0: Marker) {
        val position = p0.position
        p0.snippet = "${position.getLatitudeDegrees()} ${position.getLongitudeDegrees()}"
        p0.showInfoWindow()
    }

    override fun onMarkerDragEnd(p0: Marker) {
        viewModel.selectedDestination.value = p0.position.toLocation()
    }

}