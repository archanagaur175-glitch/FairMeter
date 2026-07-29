package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.time.LocalTime

/**
 * Chennai auto-rickshaw tariff.
 * Last verified: NOT YET VERIFIED — see TODO below.
 *
 * Sources:
 *   Official (2013, never revised): ₹25 for 1.8 km, ₹12/km thereafter
 *     — TN Transport Dept, last official notification
 *   Union-unilateral (Feb 2025): ₹50 for 1.8 km, ₹18/km
 *     — DT Next, Times Now, Jan 2025
 *   Undocumented baseline cited in research: ₹13-15/km
 *
 * // TODO: VERIFY — Confirm against current TN Transport Dept notification before real-world use.
 * // https://tnsta.gov.in/ | https://transport.tn.gov.in/
 * // Using the LAST OFFICIALLY NOTIFIED 2013 rates as default.
 * // The 2013 notification is the most recent government-authorised tariff.
 *
 * Rates (default — 2013 official):
 *   Base: ₹25 flat for first 1.8 km
 *   Per-km: ₹12/km thereafter (← CONFIRM: ₹12 or ₹18 or ₹13?)
 *   Waiting: ₹3.5 per 5 min
 *   Night (11 PM – 5 AM): 1.5× fare
 */
object ChennaiFare : FareRuleSet {
    override val city = City.CHENNAI
    override val effectiveDate = "2013 (LAST OFFICIAL GOVT NOTIFICATION — NOT REVISED SINCE)"
    override val lastVerified = "UNVERIFIED — SEE TODO IN SOURCE"

    // TODO: VERIFY THESE VALUES
    private const val MIN_FARE = 25
    private const val MIN_DIST_KM = 1.8
    private const val PER_KM = 12.0
    private const val WAITING_PER_5M = 3.5
    private val NIGHT_START = LocalTime.of(23, 0)
    private val NIGHT_END = LocalTime.of(5, 0)

    override fun baseFare(): Int = MIN_FARE
    override fun minDistanceKm(): Double = MIN_DIST_KM
    override fun perKmRate(): Double = PER_KM
    override fun nightMultiplier(): Double = 1.5

    override fun isNight(time: LocalTime): Boolean {
        return time >= NIGHT_START || time < NIGHT_END
    }

    override fun waitingFare(seconds: Int): Int {
        val blocks = seconds / 300
        val remainder = seconds % 300
        val totalBlocks = if (remainder > 0) blocks + 1 else blocks
        return (totalBlocks * WAITING_PER_5M).toInt()
    }
}
