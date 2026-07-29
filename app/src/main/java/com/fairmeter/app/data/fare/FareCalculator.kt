package com.fairmeter.app.data.fare

import com.fairmeter.app.data.fare.cities.BengaluruFare
import com.fairmeter.app.data.fare.cities.ChennaiFare
import com.fairmeter.app.data.fare.cities.DelhiFare
import com.fairmeter.app.data.fare.cities.HyderabadFare
import com.fairmeter.app.data.fare.cities.MumbaiFare
import com.fairmeter.app.data.fare.cities.roundToNearestRupee
import com.fairmeter.app.data.model.City
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

object FareCalculator {

    private fun Double.toBD(): BigDecimal = BigDecimal(this.toString())
    private fun Int.toBD(): BigDecimal = BigDecimal(this)
    private fun BigDecimal.toHalfUpInt(): Int = setScale(0, RoundingMode.HALF_UP).toInt()

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
        val effectivePerKmBD: BigDecimal

        if (city == City.HYDERABAD && isNightNow) {
            effectivePerKmBD = HyderabadFare.perKmRateNight().toBD()
        } else {
            effectivePerKmBD = rules.perKmRate().toBD()
        }

        val distanceFareRaw: Int
        if (totalDistanceKm <= rules.minDistanceKm()) {
            distanceFareRaw = 0
        } else {
            val rawBD = tickDistanceKm.toBD().multiply(effectivePerKmBD)
            distanceFareRaw = if (city == City.MUMBAI) {
                rawBD.roundToNearestRupee()
            } else {
                rawBD.toHalfUpInt()
            }
        }

        val waitingFare = rules.waitingFare(tickWaitingSeconds)
        val incrementTotal = distanceFareRaw + waitingFare

        val nightSurcharge: Int
        if (isNightNow && city != City.HYDERABAD) {
            val surchargeBD = incrementTotal.toBD()
                .multiply(rules.nightMultiplier().toBD().subtract(BigDecimal.ONE))
            nightSurcharge = surchargeBD.toHalfUpInt()
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
        val baseFare = rules.baseFare()

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
        val effectivePerKmBD: BigDecimal = if (city == City.HYDERABAD && isNightNow) {
            HyderabadFare.perKmRateNight().toBD()
        } else {
            rules.perKmRate().toBD()
        }

        val baseFare = rules.baseFare()
        val distanceBeyondMinBD = distanceKm.toBD()
            .subtract(rules.minDistanceKm().toBD())
            .max(BigDecimal.ZERO)

        val rawDistanceFare: Int = if (city == City.MUMBAI) {
            distanceBeyondMinBD.multiply(effectivePerKmBD).roundToNearestRupee()
        } else {
            distanceBeyondMinBD.multiply(effectivePerKmBD).toHalfUpInt()
        }

        val waitingFare = rules.waitingFare(waitingSeconds)
        val subtotal = baseFare + rawDistanceFare + waitingFare

        val nightSurcharge: Int
        if (isNightNow && city != City.HYDERABAD) {
            val surchargeBD = subtotal.toBD()
                .multiply(rules.nightMultiplier().toBD().subtract(BigDecimal.ONE))
            nightSurcharge = surchargeBD.toHalfUpInt()
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
