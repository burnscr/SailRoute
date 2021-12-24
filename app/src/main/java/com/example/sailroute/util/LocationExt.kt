package com.example.sailroute.util

import android.location.Location
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Returns a [Location] representation of this [LatLng].
 */
fun LatLng.toLocation(): Location {
    return Location(LocationManager.GPS_PROVIDER).apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
    }
}

/**
 * Returns a [LatLng] representation of this [Location].
 */
fun Location.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}

fun LatLng.getLatitudeDegrees(): String {
    // https://stackoverflow.com/a/38551230

    val builder = StringBuilder()

    builder.append(if (this.latitude < 0) "S " else "N ")

    val latitudeDegrees = Location.convert(abs(this.latitude), Location.FORMAT_SECONDS)
    val latitudeSplit = latitudeDegrees.split(":")

    builder.append(latitudeSplit[0]).append("°")
    builder.append(latitudeSplit[1]).append("'")
    builder.append(latitudeSplit[2].toDouble().roundToInt()).append("\"")

    return builder.toString()
}

fun LatLng.getLongitudeDegrees(): String {
    // https://stackoverflow.com/a/38551230

    val builder = StringBuilder()

    builder.append(if (this.longitude < 0) "W " else "E ")

    val longitudeDegrees = Location.convert(abs(this.longitude), Location.FORMAT_SECONDS)
    val longitudeSplit = longitudeDegrees.split(":")

    builder.append(longitudeSplit[0]).append("°")
    builder.append(longitudeSplit[1]).append("'")
    builder.append(longitudeSplit[2].toDouble().roundToInt()).append("\"")

    return builder.toString()
}