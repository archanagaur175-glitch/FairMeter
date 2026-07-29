package com.fairmeter.app.data.fare.cities

import com.fairmeter.app.data.fare.FareRuleSet
import com.fairmeter.app.data.model.City
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

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
        val perMinuteRate = BigDecimal("0.10").multiply(BigDecimal(PER_KM_BASE.toString()))
        val minutes = BigDecimal(seconds).divide(BigDecimal(60), 10, RoundingMode.HALF_UP)
        return perMinuteRate.multiply(minutes).setScale(0, RoundingMode.HALF_UP).toInt()
    }
}

fun BigDecimal.roundToNearestRupee(): Int =
    setScale(0, RoundingMode.HALF_UP).toInt()

fun Double.roundToNearestRupee(): Int =
    BigDecimal(this.toString()).roundToNearestRupee()
