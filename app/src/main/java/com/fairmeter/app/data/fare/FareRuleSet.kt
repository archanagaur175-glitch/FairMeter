package com.fairmeter.app.data.fare

import com.fairmeter.app.data.model.City
import java.time.LocalTime

interface FareRuleSet {
    val city: City
    val effectiveDate: String
    val lastVerified: String

    fun baseFare(): Int
    fun minDistanceKm(): Double
    fun perKmRate(): Double
    fun waitingFare(seconds: Int): Int
    fun nightMultiplier(): Double
    fun isNight(time: LocalTime): Boolean
}
