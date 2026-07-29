package com.fairmeter.app.domain

object EstimateRoute {
    private const val ROUTING_FACTOR = 1.3

    fun estimatedRouteDistance(sourceLat: Double, sourceLng: Double, destLat: Double, destLng: Double): Double {
        val straightLine = Haversine.distance(sourceLat, sourceLng, destLat, destLng)
        return straightLine * ROUTING_FACTOR
    }
}
