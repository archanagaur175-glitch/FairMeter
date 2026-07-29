package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Mumbai auto-rickshaw (CNG) tariff.
 * Last verified: Feb 2025 — subject to official revision.
 *
 * Source: Maharashtra Motor Vehicle Dept official tariff card (PDF)
 *   https://transport.maharashtra.gov.in/Site/Upload/GR/Auto%20Rickshaw%20Tariff%20Card.pdf
 * Effective: Feb 1, 2025
 *
 * Rates:
 *   Base: ₹26 flat for first 1.5 km
 *   Per-km: ₹17.14/km thereafter (rounded to nearest ₹; ≤49p drop, ≥50p round up)
 *   Waiting: 10% of per-km fare per minute equivalent
 *     = 0.10 × ₹17.14/km = ₹1.714/minute
 *   Night (12 AM – 5 AM): +25% surcharge on total fare
 */
object MumbaiFare : FareRuleSet {
    override val city = City.MUMBAI
    override val effectiveDate = "2025-02-01"
    override val lastVerified = "Feb 2025, subject to official revision"

    private const val MIN_FARE = 26
    private const val MIN_DIST_KM = 1.5
    private const val PER_KM_BASE = 17.14
    private val NIGHT_START = LocalTime.of(0, 0)
    private val NIGHT_END = LocalTime.of(5, 0)

    override fun baseFare(): Int = MIN_FARE
    override fun minDistanceKm(): Double = MIN_DIST_KM
    override fun perKmRate(): Double = PER_KM_BASE
    override fun nightMultiplier(): Double = 1.25

    override fun isNight(time: LocalTime): Boolean {
        return time >= NIGHT_START && time < NIGHT_END
    }

    override fun waitingFare(seconds: Int): Int {
        val minutes = seconds / 60.0
        val raw = 0.10 * PER_KM_BASE * minutes
        return raw.roundToNearestRupee()
    }
}

/**
 * Mumbai rounding rule: ≤49 paisa drops, ≥50 paisa rounds up to next rupee.
 */
fun Double.roundToNearestRupee(): Int {
    val rounded = this.roundToInt()
    val diff = this - rounded
    return when {
        this < 0 -> rounded
        diff >= 0.5 -> rounded + 1
        diff <= -0.5 -> rounded - 1
        else -> rounded
    }
}
