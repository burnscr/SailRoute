package com.example.sailroute.sailing

import com.example.sailroute.util.Degree
import kotlin.math.abs
import kotlin.math.max

object ShipVelocity {
    private val polarData = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.1, 0.5, 0.9, 1.2, 2.0, 3.2, 4.3, 4.6, 4.8, 4.9, 4.7, 3.4, 2.1, 1.7, 1.6, 1.5, 1.4, 1.3),
        doubleArrayOf(0.0, 0.4, 0.9, 1.4, 2.2, 3.2, 4.5, 5.7, 6.1, 6.3, 6.5, 6.3, 4.9, 3.2, 2.7, 2.3, 2.2, 2.1, 2.0),
        doubleArrayOf(0.0, 0.5, 1.5, 2.0, 3.0, 4.4, 5.8, 7.0, 7.5, 7.8, 8.1, 8.0, 6.5, 4.6, 3.6, 3.1, 2.8, 2.6, 2.5),
        doubleArrayOf(0.0, 0.8, 1.9, 2.8, 4.1, 5.8, 6.9, 7.9, 8.2, 8.3, 8.5, 8.4, 7.4, 6.2, 5.1, 4.2, 3.6, 3.1, 2.8),
        doubleArrayOf(0.0, 1.0, 2.3, 3.5, 5.4, 7.1, 8.0, 8.7, 8.9, 9.0, 9.0, 8.9, 8.4, 7.8, 6.7, 5.3, 4.3, 3.6, 3.0),
        doubleArrayOf(0.0, 1.3, 2.5, 4.0, 6.0, 7.8, 8.5, 9.3, 9.4, 9.5, 9.6, 9.6, 9.2, 8.6, 7.7, 6.1, 5.0, 4.2, 3.6),
        doubleArrayOf(0.0, 1.5, 2.8, 4.5, 6.9, 8.4, 9.2, 9.8, 10.0, 10.2, 10.3, 10.3, 9.9, 9.4, 8.6, 7.0, 5.8, 4.9, 4.2),
        doubleArrayOf(0.0, 1.6, 3.0, 4.8, 7.2, 8.7, 9.4, 10.1, 10.3, 10.4, 10.6, 10.6, 10.3, 10.0, 9.4, 7.6, 6.3, 5.4, 4.8),
        doubleArrayOf(0.0, 1.6, 3.3, 5.2, 7.8, 9.1, 9.7, 10.3, 10.6, 10.7, 10.9, 11.1, 10.7, 10.4, 9.9, 8.3, 6.9, 6.0, 5.2)
    )

    fun getVelocity(windAngle: Degree, windSpeed: Double): Double {

        // Find the upper and lower known angles
        val lowerAngle: Degree = (windAngle / 10) * 10
        val upperAngle: Degree = if (windAngle != lowerAngle) lowerAngle + 10 else lowerAngle

        // Find the upper and lower known speeds
        var lowerSpeed: Int = windSpeed.toInt() / 1
        var upperSpeed: Int = if (!windSpeed.isClose(lowerSpeed.toDouble())) lowerSpeed + 1 else lowerSpeed

        // no data available for 1 knot windspeed - extrapolate between 0-2 knots instead
        if (upperSpeed == 1) upperSpeed = 2
        if (lowerSpeed == 1) lowerSpeed = 0

        // no data available for over 10 knot windspeed - extrapolate from 9-10 knots instead
        if (windSpeed > 10) {
            upperSpeed = 10
            lowerSpeed = 9
        }

        val uuData = polarData[upperSpeed][upperAngle/10]
        val ulData = polarData[upperSpeed][lowerAngle/10]
        val luData = polarData[lowerSpeed][upperAngle/10]
        val llData = polarData[lowerSpeed][lowerAngle/10]

        val angleMultiplier = multiplier(windAngle.toDouble(), upperAngle, lowerAngle)
        val speedMultiplier = multiplier(windSpeed, upperSpeed, lowerSpeed)

        val v1 = extrapolate(uuData, ulData, angleMultiplier)
        val v2 = extrapolate(luData, llData, angleMultiplier)
        return extrapolate(v1, v2, speedMultiplier)
    }

    private fun Double.isClose(other: Double): Boolean {
        val relTol = 1e-09
        val absTol = 0.0

        return abs(this - other) <= max(relTol * max(abs(this), abs(other)), absTol)
    }

    private fun multiplier(desired: Double, upper: Int, lower: Int): Double {
        val difference = upper - lower
        return if (difference != 0) (desired - lower)/difference else 0.0
    }

    private fun extrapolate(higher: Double, lower: Double, multiplier: Double): Double {
        return lower + ((higher - lower) * multiplier)
    }
}