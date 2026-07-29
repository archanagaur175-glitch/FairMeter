package com.fairmeter.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HaversineTest {

    @Test
    fun distance_zero() {
        val d = Haversine.distance(12.97, 77.59, 12.97, 77.59)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun distance_oneDegreeLat_approximately111km() {
        val d = Haversine.distance(0.0, 0.0, 1.0, 0.0)
        assertEquals(111.0, d, 2.0) // ~111 km per degree
    }

    @Test
    fun distance_bangaloreToMumbai_approximate() {
        val d = Haversine.distance(12.97, 77.59, 19.08, 72.88)
        assertEquals(845.0, d, 15.0)
    }

    @Test
    fun distance_symmetry() {
        val d1 = Haversine.distance(12.97, 77.59, 19.08, 72.88)
        val d2 = Haversine.distance(19.08, 72.88, 12.97, 77.59)
        assertEquals(d1, d2, 0.001)
    }
}
