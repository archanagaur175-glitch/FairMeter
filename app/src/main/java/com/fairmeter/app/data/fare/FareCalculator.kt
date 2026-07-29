package com.fairmeter.app.data.fare

import com.fairmeter.app.data.fare.cities.BengaluruFare
import com.fairmeter.app.data.fare.cities.ChennaiFare
import com.fairmeter.app.data.fare.cities.DelhiFare
import com.fairmeter.app.data.fare.cities.HyderabadFare
import com.fairmeter.app.data.fare.cities.MumbaiFare
import com.fairmeter.app.data.fare.cities.roundToNearestRupee
import com.fairmeter.app.data.model.City
import java.time.LocalTime
import kotlin.math.roundToInt

object FareCalculator {

    private val rulesMap: Map<City, FareRuleSet> = mapOf(
        City.BENGALURU to BengaluruFare,
        City.MUMBAI to MumbaiFare,
        City.DELHI to DelhiFare,
        City.CHENNAI to ChennaiFare,
        City.HYDERABAD to HyderabadFare
    )

    fun getRules(city: City): FareRuleSet =
        rulesMap[city] ?: throw IllegalArgumentException("No fare rules for $city")

    /**
     * Computes a fare increment for a single GPS tick (~2 seconds).
     * Night surcharge is evaluated against [currentTime] each tick,
     * correctly handling trips that cross night boundaries.
     */
    fun computeIncrement(
        totalDistanceKm: Double,
        tickDistanceKm: Double,
        tickWaitingSeconds: Int,
        currentTime: LocalTime,
        city: City
    ): FareIncrement {
        val rules = getRules(city)
        val isNightNow = rules.isNight(currentTime)
        val effectivePerKm: Double

        if (city == City.HYDERABAD && isNightNow) {
            effectivePerKm = HyderabadFare.perKmRateNight()
        } else {
            effectivePerKm = rules.perKmRate()
        }

        val distanceFareRaw: Int
        if (totalDistanceKm <= rules.minDistanceKm()) {
            distanceFareRaw = 0
        } else {
            val distance = tickDistanceKm
            if (city == City.MUMBAI) {
                distanceFareRaw = ((distance * effectivePerKm)).roundToNearestRupee()
            } else {
                distanceFareRaw = (distance * effectivePerKm).toInt()
            }
        }

        val waitingFare = rules.waitingFare(tickWaitingSeconds)
        val incrementTotal = distanceFareRaw + waitingFare

        val nightSurcharge: Int
        if (isNightNow && city != City.HYDERABAD) {
            val multiplier = rules.nightMultiplier()
            nightSurcharge = (incrementTotal * (multiplier - 1.0)).toInt()
        } else {
            nightSurcharge = 0
        }

        return FareIncrement(
            distanceFare = distanceFareRaw,
            waitingFare = waitingFare,
            nightSurcharge = nightSurcharge,
            totalIncrement = distanceFareRaw + waitingFare + nightSurcharge
        )
    }

    /**
     * Computes a complete fare breakdown from accumulated totals.
     */
    fun computeBreakdown(
        totalDistanceKm: Double,
        totalWaitingSeconds: Int,
        city: City,
        distanceFareTotal: Int,
        waitingFareTotal: Int,
        nightSurchargeTotal: Int
    ): FareBreakdown {
        val rules = getRules(city)
        val baseFare = if (totalDistanceKm <= rules.minDistanceKm()) rules.baseFare() else rules.baseFare()

        return FareBreakdown(
            baseFare = baseFare,
            distanceFare = distanceFareTotal,
            waitingFare = waitingFareTotal,
            nightSurcharge = nightSurchargeTotal,
            total = baseFare + distanceFareTotal + waitingFareTotal + nightSurchargeTotal
        )
    }

    /**
     * One-shot estimate for pre-trip quoting (uses current time for night detection).
     */
    fun estimateFare(
        distanceKm: Double,
        waitingSeconds: Int,
        currentTime: LocalTime,
        city: City
    ): FareBreakdown {
        val rules = getRules(city)
        val isNightNow = rules.isNight(currentTime)
        val effectivePerKm: Double = if (city == City.HYDERABAD && isNightNow) {
            HyderabadFare.perKmRateNight()
        } else {
            rules.perKmRate()
        }

        val baseFare = rules.baseFare()
        val distanceBeyondMin = (distanceKm - rules.minDistanceKm()).coerceAtLeast(0.0)

        val rawDistanceFare: Int
        if (city == City.MUMBAI) {
            rawDistanceFare = (distanceBeyondMin * effectivePerKm).roundToNearestRupee()
        } else {
            rawDistanceFare = (distanceBeyondMin * effectivePerKm).toInt()
        }

        val waitingFare = rules.waitingFare(waitingSeconds)
        val subtotal = baseFare + rawDistanceFare + waitingFare

        val nightSurcharge: Int
        if (isNightNow && city != City.HYDERABAD) {
            nightSurcharge = (subtotal * (rules.nightMultiplier() - 1.0)).toInt()
        } else {
            nightSurcharge = 0
        }

        return FareBreakdown(
            baseFare = baseFare,
            distanceFare = rawDistanceFare,
            waitingFare = waitingFare,
            nightSurcharge = nightSurcharge,
            total = subtotal + nightSurcharge
        )
    }
}
