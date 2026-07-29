package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.time.LocalTime

/**
 * Delhi STA (State Transport Authority) auto-rickshaw tariff.
 * Last verified: Jan 2025 — subject to official revision.
 *
 * Source: https://sta.delhi.gov.in
 * Effective: Jan 9, 2025
 *
 * Rates:
 *   Base: ₹30 flat for first 1.5 km
 *   Per-km: ₹11/km thereafter
 *   Waiting: ₹1/min after first 15 min free
 *   Night (11 PM – 5 AM): +25% surcharge
 */
object DelhiFare : FareRuleSet {
    override val city = City.DELHI
    override val effectiveDate = "2025-01-09"
    override val lastVerified = "Jan 2025, subject to official revision"

    private const val MIN_FARE = 30
    private const val MIN_DIST_KM = 1.5
    private const val PER_KM = 11.0
    private const val WAITING_FREE_SECONDS = 900
    private const val WAITING_PER_MIN = 1
    private val NIGHT_START = LocalTime.of(23, 0)
    private val NIGHT_END = LocalTime.of(5, 0)

    override fun baseFare(): Int = MIN_FARE
    override fun minDistanceKm(): Double = MIN_DIST_KM
    override fun perKmRate(): Double = PER_KM
    override fun nightMultiplier(): Double = 1.25

    override fun isNight(time: LocalTime): Boolean {
        return time >= NIGHT_START || time < NIGHT_END
    }

    override fun waitingFare(seconds: Int): Int {
        val chargeableSeconds = (seconds - WAITING_FREE_SECONDS).coerceAtLeast(0)
        return chargeableSeconds / 60 * WAITING_PER_MIN
    }
}
