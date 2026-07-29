package com.fairmeter.app.data.model

sealed class TripState {
    data object Idle : TripState()
    data class PreTrip(
        val city: City,
        val sourceLat: Double,
        val sourceLng: Double,
        val destLat: Double,
        val destLng: Double
    ) : TripState()
    data class Active(
        val city: City,
        val startTime: Long,
        val accumulatedDistanceKm: Double = 0.0,
        val accumulatedWaitingSeconds: Int = 0,
        val totalFare: Int = 0,
        val isWaiting: Boolean = false
    ) : TripState()
    data class Paused(
        val city: City,
        val startTime: Long,
        val accumulatedDistanceKm: Double,
        val accumulatedWaitingSeconds: Int,
        val totalFare: Int
    ) : TripState()
    data class Ended(
        val city: City,
        val startTime: Long,
        val endTime: Long,
        val totalDistanceKm: Double,
        val totalWaitingSeconds: Int,
        val baseFare: Int,
        val distanceFare: Int,
        val waitingFare: Int,
        val nightSurcharge: Int,
        val totalFare: Int
    ) : TripState()
}
