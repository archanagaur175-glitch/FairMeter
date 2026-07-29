package com.fairmeter.app.data.tracking

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * Fuses accelerometer variance with GPS-derived velocity to detect "waiting" state.
 * Declares waiting when both conditions hold for >= WAITING_THRESHOLD_SECONDS:
 *   1. Speed < 1 km/h (~0.3 m/s)
 *   2. Accelerometer variance near-zero (vehicle stationary)
 *
 * Resumes MOVING on sustained movement > 1 m/s for >= MOVING_THRESHOLD_SECONDS.
 */
class WaitingDetector(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private val _waitingState = MutableStateFlow(false)
    val waitingState: StateFlow<Boolean> = _waitingState

    private var isWaiting = false
    private var waitingStartTime = 0L
    private var movingStartTime = 0L
    private var lastVelocityMs = 0.0

    // Accelerometer ring buffer for variance computation
    private val accelSamples = mutableListOf<Float>()
    private val maxAccelSamples = 50
    private var lastAccelNs = 0L

    companion object {
        private const val SPEED_WAITING_THRESHOLD_MS = 0.3
        private const val SPEED_MOVING_THRESHOLD_MS = 1.0
        private const val WAITING_THRESHOLD_MS = 20_000L
        private const val MOVING_THRESHOLD_MS = 5_000L
        private const val ACCEL_VARIANCE_THRESHOLD = 0.5f
    }

    fun start() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        reset()
    }

    fun reset() {
        isWaiting = false
        waitingStartTime = 0
        movingStartTime = 0
        lastVelocityMs = 0.0
        accelSamples.clear()
        _waitingState.value = false
    }

    /**
     * Called from the tracking loop with the current GPS-derived speed (m/s).
     */
    fun onSpeedUpdate(speedMs: Float) {
        lastVelocityMs = speedMs.toDouble()
        evaluate()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            accelSamples.add(magnitude)
            if (accelSamples.size > maxAccelSamples) {
                accelSamples.removeAt(0)
            }
            evaluate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun evaluate() {
        val speedLow = lastVelocityMs < SPEED_WAITING_THRESHOLD_MS
        val accelVariance = computeVariance(accelSamples)
        val accelFlat = accelSamples.size > 10 && accelVariance < ACCEL_VARIANCE_THRESHOLD
        val now = System.currentTimeMillis()

        if (speedLow && accelFlat) {
            if (!isWaiting) {
                if (waitingStartTime == 0L) {
                    waitingStartTime = now
                }
                if (now - waitingStartTime >= WAITING_THRESHOLD_MS) {
                    isWaiting = true
                    movingStartTime = 0L
                    _waitingState.value = true
                }
            }
        } else {
            waitingStartTime = 0L
            if (isWaiting) {
                if (movingStartTime == 0L) {
                    movingStartTime = now
                }
                if (now - movingStartTime >= MOVING_THRESHOLD_MS) {
                    isWaiting = false
                    waitingStartTime = 0L
                    movingStartTime = 0L
                    _waitingState.value = false
                }
            }
        }
    }

    private fun computeVariance(samples: MutableList<Float>): Float {
        if (samples.isEmpty()) return Float.MAX_VALUE
        val mean = samples.average()
        return samples.map { (it - mean) * (it - mean) }.average().toFloat()
    }
}
