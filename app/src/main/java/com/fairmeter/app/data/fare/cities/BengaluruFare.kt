package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.time.LocalTime

/**
 * Bengaluru DTA (Directorate of Transport Authorities) auto-rickshaw tariff.
 * Last verified: Aug 2025 — subject to official revision.
 *
 * Source: https://www.karnataka.gov.in/transport
 * Effective: Aug 1, 2025
 *
 * Rates:
 *   Base: ₹36 flat for first 2 km
 *   Per-km: ₹18/km thereafter
 *   Waiting: First 5 min free, then ₹10 per 15 min
 *   Night (10 PM – 5 AM): 1.5× total fare
 */
object BengaluruFare : FareRuleSet {
    override val city = City.BENGALURU
    override val effectiveDate = "2025-08-01"
    override val lastVerified = "Jul 2025, subject to official revision"

    private const val MIN_FARE = 36
    private const val MIN_DIST_KM = 2.0
    private const val PER_KM = 18.0
    private const val WAITING_FREE_SECONDS = 300
    private const val WAITING_PER_15M = 10
    private val NIGHT_START = LocalTime.of(22, 0)
    private val NIGHT_END = LocalTime.of(5, 0)

    override fun baseFare(): Int = MIN_FARE
    override fun minDistanceKm(): Double = MIN_DIST_KM
    override fun perKmRate(): Double = PER_KM
    override fun nightMultiplier(): Double = 1.5

    override fun isNight(time: LocalTime): Boolean {
        return time >= NIGHT_START || time < NIGHT_END
    }

    override fun waitingFare(seconds: Int): Int {
        val chargeableSeconds = (seconds - WAITING_FREE_SECONDS).coerceAtLeast(0)
        val blocks = chargeableSeconds / 900
        val remainder = chargeableSeconds % 900
        val extraBlock = if (remainder > 0) 1 else 0
        return (blocks + extraBlock) * WAITING_PER_15M
    }
}
