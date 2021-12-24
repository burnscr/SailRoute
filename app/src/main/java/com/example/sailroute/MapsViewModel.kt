package com.example.sailroute

import android.location.Location
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sailroute.sailing.Route

class MapsViewModel: ViewModel() {

    val windHeading = MutableLiveData<Int?>()
    val windVelocity = MutableLiveData<Double?>()
    val requestingLocationUpdates = ObservableBoolean(false)
    val lastKnownLocation = MutableLiveData<Location>()
    val selectedDestination = MutableLiveData<Location>()

    /** Determines if route selection goes clockwise or counter clockwise. */
    val primaryIsSelected = MutableLiveData(true)

    val lastRoutes = MutableLiveData<List<Route>>()
    val currentRoute = MutableLiveData<Route?>()
    val primaryIconId = MutableLiveData<Int?>()
    val primaryVisible = ObservableBoolean(true)
    val secondaryVisible = ObservableBoolean(false)

    fun setIconId(id: Int?) {
        if (id != primaryIconId.value)
            primaryIconId.value = id
    }

    fun setPrimaryVisible(visible: Boolean) {
        if (visible != primaryVisible.get())
            primaryVisible.set(visible)
    }

    fun setSecondaryVisible(visible: Boolean) {
        if (visible != secondaryVisible.get())
            secondaryVisible.set(visible)
    }

}
