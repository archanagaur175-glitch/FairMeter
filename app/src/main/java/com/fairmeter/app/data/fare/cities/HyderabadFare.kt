package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.time.LocalTime

/**
 * Hyderabad Telangana Transport Dept auto-rickshaw tariff.
 * Last verified: 2025 — subject to official revision.
 *
 * Source: https://transport.telangana.gov.in
 *
 * Rates:
 *   Base: ₹20 flat for first 2 km
 *   Per-km day: ₹11/km thereafter
 *   Per-km night: ₹17/km thereafter
 *   Waiting: (not officially specified — using ₹1/min as reasonable default)
 *   Night (11 PM – 5 AM): 1.5× fare (applied via higher per-km rate)
 */
object HyderabadFare : FareRuleSet {
    override val city = City.HYDERABAD
    override val effectiveDate = "2025"
    override val lastVerified = "2025, subject to official revision"

    private const val MIN_FARE = 20
    private const val MIN_DIST_KM = 2.0
    private const val PER_KM_DAY = 11.0
    private const val PER_KM_NIGHT = 17.0
    private const val WAITING_PER_MIN = 1
    private val NIGHT_START = LocalTime.of(23, 0)
    private val NIGHT_END = LocalTime.of(5, 0)

    override fun baseFare(): Int = MIN_FARE
    override fun minDistanceKm(): Double = MIN_DIST_KM

    /**
     * Hyderabad uses different day/night per-km rates rather than a surcharge.
     * The perKmRate() returns the day rate; nightMultiplier() at 1.0 since
     * the night rate is already the higher figure.
     */
    override fun perKmRate(): Double = PER_KM_DAY
    override fun nightMultiplier(): Double = 1.0

    fun perKmRateNight(): Double = PER_KM_NIGHT

    override fun isNight(time: LocalTime): Boolean {
        return time >= NIGHT_START || time < NIGHT_END
    }

    override fun waitingFare(seconds: Int): Int {
        return seconds / 60 * WAITING_PER_MIN
    }
}
