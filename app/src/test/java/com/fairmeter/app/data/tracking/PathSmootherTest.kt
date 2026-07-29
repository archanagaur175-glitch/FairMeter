package com.fairmeter.app.data.tracking

import com.fairmeter.app.data.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PathSmootherTest {

    @Test
    fun smooth_initialPoint_returnsAsIs() {
        val smoother = PathSmoother()
        val point = LocationPoint(12.97, 77.59, 5.0f, 90f, 1000L, 8f)
        val result = smoother.smooth(point)
        assertNotNull(result)
        assertEquals(12.97, result!!.latitude, 0.001)
        assertEquals(77.59, result.longitude, 0.001)
    }

    @Test
    fun smooth_secondPoint_smoothesCoordinates() {
        val smoother = PathSmoother()
        smoother.smooth(LocationPoint(12.97, 77.59, 5f, 90f, 1000L, 8f))
        val result = smoother.smooth(LocationPoint(12.971, 77.591, 5.5f, 90f, 3000L, 8f))
        assertNotNull(result)
    }

    @Test
    fun smooth_spikeSpeedRejected() {
        val smoother = PathSmoother()
        smoother.smooth(LocationPoint(12.97, 77.59, 5f, 90f, 1000L, 8f))

        // 100 m/s = 360 km/h — clearly a spike
        val result = smoother.smooth(LocationPoint(12.98, 77.60, 100f, 90f, 3000L, 8f))
        assertNull(result)
    }

    @Test
    fun smooth_consecutiveLowSpeedNotSpike() {
        val smoother = PathSmoother()
        smoother.smooth(LocationPoint(12.97, 77.59, 5f, 90f, 1000L, 8f))
        val result = smoother.smooth(LocationPoint(12.971, 77.591, 5.5f, 90f, 3000L, 8f))
        assertNotNull(result)
    }

    @Test
    fun reset_clearsState() {
        val smoother = PathSmoother()
        smoother.smooth(LocationPoint(12.97, 77.59, 5f, 90f, 1000L, 8f))
        smoother.reset()
        val result = smoother.smooth(LocationPoint(12.98, 77.60, 6f, 90f, 2000L, 8f))
        assertNotNull(result) // Should initialize fresh
        assertEquals(12.98, result!!.latitude, 0.001)
    }
}
