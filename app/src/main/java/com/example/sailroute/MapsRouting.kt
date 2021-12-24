package com.example.sailroute

import android.graphics.Color
import android.location.Location
import com.example.sailroute.sailing.Route
import com.example.sailroute.util.toLatLng
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil

class MapsRouting(private val map: GoogleMap) {

    private val polylines = ArrayList<Polyline>()

//    init {
//        val polylineOptions = PolylineOptions()
//        polyline = map.addPolyline(polylineOptions)
//        polyline.pattern = listOf(
//            Dot(), Gap(20F), Dash(30F), Gap(20F)
//        )
//        polyline.jointType = JointType.ROUND
//    }

    fun setOrUpdateRoute(routes: List<Route>, location: Location, primaryRouteSelected: Boolean) {
        val colorPalette = if (primaryRouteSelected || routes.size == 1) listOf(Color.BLUE, Color.GRAY) else listOf(Color.GRAY, Color.BLUE)

        removeAllPolylines()

        if (routes.isNotEmpty()) {
            addRoute(routes[0], location, colorPalette[0], primaryRouteSelected)
            if (routes.size > 1) {
                addRoute(routes[1], location, colorPalette[1], !primaryRouteSelected)
            }
        }
    }

    private fun addRoute(route: Route, location: Location, color: Int, isPrimary: Boolean) {
        val positions: ArrayList<LatLng> = arrayListOf()
        val directRoute = route.arrivalDistance <= 0

        val startPoint = location.toLatLng()
        val midPoint = SphericalUtil.computeOffset(startPoint, route.departureDistance, route.departureHeading.toDouble())
        val endPoint = SphericalUtil.computeOffset(midPoint, route.arrivalDistance, route.arrivalHeading.toDouble())

        positions.add(startPoint)
        positions.add(midPoint)

        if (!directRoute)
            positions.add(endPoint)

        val polylineOptions = PolylineOptions()
            .jointType(JointType.ROUND)
            .color(color)
            .clickable(!isPrimary)
        val polyline = map.addPolyline(polylineOptions)
        polyline.pattern = listOf(
            Gap(20F), Dash(30F)
        )
        polyline.points = positions

        polylines.add(polyline)
    }

    private fun removeAllPolylines() {
        for (polyline in polylines)
            polyline.remove()
        polylines.clear()
    }

}