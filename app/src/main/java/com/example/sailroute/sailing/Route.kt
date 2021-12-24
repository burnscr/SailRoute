package com.example.sailroute.sailing

data class Route(
    val departureHeading: Int,
    val arrivalHeading: Int,
    val departureWindBearing: Int,
    val arrivalWindBearing: Int,
    val departureDistance: Double,
    val arrivalDistance: Double,
    val duration: Double
) {
    /** Returns the total trip distance in meters. */
    fun totalDistance(): Double = departureDistance + arrivalDistance
}