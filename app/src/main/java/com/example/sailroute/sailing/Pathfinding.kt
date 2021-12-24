package com.example.sailroute.sailing

import com.example.sailroute.util.Degree
import com.example.sailroute.util.bindTo360
import com.example.sailroute.util.toRadian
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

object Pathfinding {

    private lateinit var velocityCache: DoubleArray
    private var cachedWindSpeed: Double? = null
    private var cachedWindAngle: Int? = null

    fun optimalRoutes(
        destinationHeading: Int,
        destinationDistance: Double,
        windHeading: Int,
        windSpeed: Double
    ): List<Route> {
        val bestRoutes = ArrayList<Route>()

        val destinationOppositeHeading = oppositeHeading(destinationHeading)

        // populate or update velocity cache
        updateVelocityCache(windHeading, windSpeed)

        // calculate straight line duration for baseline
        val straightVelocity = velocityCache[destinationHeading]
        val straightDuration = if (straightVelocity > 0) destinationDistance/knotsToMetersPerSecond(straightVelocity) else -1.0
        val straightRelWindBearing = headingToRelativeWindAngle(destinationHeading, windHeading)

        bestRoutes.add(Route(
            destinationHeading, destinationHeading,
            straightRelWindBearing, straightRelWindBearing,
            destinationDistance, 0.0, straightDuration))

        for (departureHeading in 0 until 360) {
            if (isValidDepartureHeading(departureHeading, destinationHeading, destinationOppositeHeading)) {

                val departureAngle = angleDistance(destinationHeading, departureHeading)
                val departureVelocity = velocityCache[departureHeading]

                if (!departureVelocity.isClose(0.0) && departureVelocity > 0) {

                    // Iterate over each valid arrival heading
                    val headingIncrement = closestHeadingDirection(destinationOppositeHeading, departureHeading)
                    var heading = (destinationOppositeHeading + headingIncrement).bindTo360()

                    while (heading != departureHeading) {

                        val arrivalHeading = oppositeHeading(heading)
                        val arrivalAngle = angleDistance(destinationOppositeHeading, heading)
                        val arrivalVelocity = velocityCache[arrivalHeading]

                        if (!arrivalVelocity.isClose(0.0) && arrivalVelocity > 0) {

                            updateRoutes(
                                bestRoutes, destinationDistance,
                                arrivalAngle, departureAngle,
                                arrivalHeading, departureHeading,
                                arrivalVelocity, departureVelocity,
                                windHeading
                            )

                        }
                        heading = (heading + headingIncrement).bindTo360()
                    }
                }
            }
        }

        return bestRoutes
    }

    private fun updateRoutes(
        routes: ArrayList<Route>, destinationDistance: Double,
        arrivalAngle: Degree, departureAngle: Degree,
        arrivalHeading: Degree, departureHeading: Degree,
        arrivalVelocity: Double, departureVelocity: Double,
        windHeading: Degree
    ) {

        val tackAngle = 180 - arrivalAngle - departureAngle
        val trigStuff = destinationDistance / sin(tackAngle.toRadian())
        val arrivalDistance = trigStuff * sin(departureAngle.toRadian())
        val departureDistance = trigStuff * sin(arrivalAngle.toRadian())
        val arrivalDuration = arrivalDistance / knotsToMetersPerSecond(arrivalVelocity)         // seconds
        val departureDuration = departureDistance / knotsToMetersPerSecond(departureVelocity)   // seconds
        val tripDuration = departureDuration + arrivalDuration

        val departureRelWindBearing = headingToRelativeWindAngle(departureHeading, windHeading)
        val arrivalRelWindBearing = headingToRelativeWindAngle(arrivalHeading, windHeading)

        val route = Route(
            departureHeading,
            arrivalHeading,
            departureRelWindBearing,
            arrivalRelWindBearing,
            departureDistance,
            arrivalDistance,
            tripDuration
        )

        // if this new route is better than the ones in routes, replace all routes with this new one
        if (routes[0].duration < 0 || tripDuration < routes[0].duration) {
            routes.clear()
            routes.add(route)
        } else if (tripDuration.isClose(routes[0].duration)) {
            routes.add(route)
        }
    }

    private fun headingToRelativeWindAngle(heading: Degree, windHeading: Degree): Degree {
        val magnitude = angleDistance(heading, windHeading)
        val sign = closestHeadingDirection(windHeading, heading)
        return sign * magnitude
    }

    private fun closestHeadingDirection(alpha: Degree, beta: Degree): Int {
        val initialAngle = angleDistance(alpha, beta)
        val incrementedAngle = angleDistance((alpha + 1).bindTo360(), beta)
        return if (incrementedAngle < initialAngle) 1 else -1
    }

    private fun isValidDepartureHeading(departureHeading: Degree, destinationHeading: Degree, destinationOppositeHeading: Degree): Boolean {
        return departureHeading != destinationHeading && departureHeading != destinationOppositeHeading
    }

    private fun updateVelocityCache(windHeading: Degree, windSpeed: Double) {
        if (!this::velocityCache.isInitialized
            || !cachedWindSpeed!!.isClose(windSpeed)
            || cachedWindAngle != windHeading) {
            velocityCache = DoubleArray(360) { heading: Int ->
                val windAngle = angleDistance(heading, windHeading)
                ShipVelocity.getVelocity(windAngle, windSpeed)
            }
            cachedWindSpeed = windSpeed
            cachedWindAngle = windHeading
        }
    }

    private fun angleDistance(alpha: Degree, beta: Degree): Int {
        val phi = abs(beta - alpha).bindTo360()
        return if (phi > 180) 360 - phi else phi
    }

    private fun oppositeHeading(heading: Degree): Int = (heading - 180).bindTo360()

    private fun Double.isClose(other: Double): Boolean {
        val relTol = 1e-09
        val absTol = 0.0

        return abs(this - other) <= max(relTol * max(abs(this), abs(other)), absTol)
    }

    private fun knotsToMetersPerSecond(knots: Double): Double = knots / 1.944

}