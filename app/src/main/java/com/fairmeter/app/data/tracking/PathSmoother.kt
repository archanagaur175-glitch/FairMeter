package com.fairmeter.app.data.tracking

import com.fairmeter.app.data.model.LocationPoint
import kotlin.math.abs

/**
 * 1D Kalman filter for GPS latitude, longitude, and speed.
 * Independent filters per dimension. Tuned for urban auto-rickshaw
 * speeds (0–40 km/h). Rejects points with speed > 80 km/h as spikes.
 */
class PathSmoother {

    private var latFilter = KalmanFilter1D()
    private var lngFilter = KalmanFilter1D()
    private var speedFilter = KalmanFilter1D()
    private var lastSmoothed: LocationPoint? = null

    // Process noise — allows up to ~10 m/s^2 acceleration
    private val processNoise = 10.0
    // Observation noise — GPS accuracy ~8m typical
    private val observationNoise = 8.0
    private val maxPlausibleSpeed = 22.0 // ~80 km/h in m/s

    /**
     * Returns a smoothed [LocationPoint] or null if the raw point is rejected as a spike.
     */
    fun smooth(raw: LocationPoint): LocationPoint? {
        val speedMs = raw.speed.toDouble()

        if (speedMs > maxPlausibleSpeed && lastSmoothed != null) {
            return null
        }

        val smoothedLat = latFilter.update(raw.latitude, processNoise, observationNoise)
        val smoothedLng = lngFilter.update(raw.longitude, processNoise, observationNoise)
        val smoothedSpeed = speedFilter.update(speedMs, processNoise, observationNoise)

        val result = LocationPoint(
            latitude = smoothedLat,
            longitude = smoothedLng,
            speed = smoothedSpeed.toFloat(),
            bearing = raw.bearing,
            timestamp = raw.timestamp,
            accuracy = raw.accuracy
        )
        lastSmoothed = result
        return result
    }

    fun reset() {
        latFilter = KalmanFilter1D()
        lngFilter = KalmanFilter1D()
        speedFilter = KalmanFilter1D()
        lastSmoothed = null
    }

    private class KalmanFilter1D {
        private var x = 0.0
        private var p = 1000.0
        private var initialized = false

        fun update(measurement: Double, processNoise: Double, observationNoise: Double): Double {
            if (!initialized) {
                x = measurement
                p = observationNoise
                initialized = true
                return x
            }

            val pPred = p + processNoise
            val k = pPred / (pPred + observationNoise)
            x = x + k * (measurement - x)
            p = (1 - k) * pPred
            return x
        }
    }
}
