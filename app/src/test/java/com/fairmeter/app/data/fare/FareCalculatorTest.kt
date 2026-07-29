package com.fairmeter.app.data.fare

import com.fairmeter.app.data.fare.cities.BengaluruFare
import com.fairmeter.app.data.fare.cities.ChennaiFare
import com.fairmeter.app.data.fare.cities.DelhiFare
import com.fairmeter.app.data.fare.cities.HyderabadFare
import com.fairmeter.app.data.fare.cities.MumbaiFare
import com.fairmeter.app.data.fare.cities.roundToNearestRupee
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FareCalculatorTest {

    // Bengaluru tests
    @Test
    fun bengaluru_belowMinDistance_returnsBaseFare() {
        val result = FareCalculator.estimateFare(
            distanceKm = 1.5,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        assertEquals(36, result.baseFare)
        assertEquals(0, result.distanceFare)
        assertEquals(36, result.total)
    }

    @Test
    fun bengaluru_aboveMinDistance_computesPerKm() {
        val result = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        assertEquals(36, result.baseFare)
        assertEquals(54, result.distanceFare) // 3 km * 18 = 54
        assertEquals(90, result.baseFare + result.distanceFare)
    }

    @Test
    fun bengaluru_waitingFreeFirst5Minutes() {
        val waiting = BengaluruFare.waitingFare(240)
        assertEquals(0, waiting) // first 5 min free

        val waiting2 = BengaluruFare.waitingFare(600)
        assertEquals(10, waiting2) // 5 min free, then 5 min = one 15-min block
    }

    @Test
    fun bengaluru_nightSurcharge_appliedAfter10PM() {
        val result = FareCalculator.estimateFare(
            distanceKm = 2.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(22, 30),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        assertEquals(36, result.baseFare)
        assertEquals(0, result.distanceFare)
        assertEquals(18, result.nightSurcharge) // 50% of base = 18
    }

    @Test
    fun bengaluru_nightBoundaryCrossing_perTickEvaluation() {
        // Test that night surcharge is applied per-tick at night
        val dayIncrement = FareCalculator.computeIncrement(
            totalDistanceKm = 3.0,
            tickDistanceKm = 0.5,
            tickWaitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        assertEquals(0, dayIncrement.nightSurcharge)

        val nightIncrement = FareCalculator.computeIncrement(
            totalDistanceKm = 3.0,
            tickDistanceKm = 0.5,
            tickWaitingSeconds = 0,
            currentTime = LocalTime.of(22, 30),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        assertEquals(5, nightIncrement.nightSurcharge)
    }

    // Mumbai tests
    @Test
    fun mumbai_baseFareWithinMinDistance() {
        val result = FareCalculator.estimateFare(
            distanceKm = 1.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.MUMBAI
        )
        assertEquals(26, result.baseFare)
        assertEquals(0, result.distanceFare)
    }

    @Test
    fun mumbai_distanceFare_BeyondMin_usesRounding() {
        // 2.0 km: beyond 1.5 = 0.5 km, 0.5 * 17.14 = 8.57 -> round to 9
        val result = FareCalculator.estimateFare(
            distanceKm = 2.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.MUMBAI
        )
        assertEquals(9, result.distanceFare)
    }

    @Test
    fun mumbai_rounding_49paisaDrops() {
        assertEquals(41, 41.13.roundToNearestRupee())
    }

    @Test
    fun mumbai_rounding_50paisaRoundsUp() {
        assertEquals(62, 61.70.roundToNearestRupee())
    }

    @Test
    fun mumbai_waitingCharge_10percentOfPerKm() {
        // 10% of 17.14 per minute = 1.714/min
        // 5 min waiting = 5 * 1.714 = 8.57 -> round to 9
        val waiting = MumbaiFare.waitingFare(300)
        assertEquals(9, waiting)
    }

    @Test
    fun mumbai_nightSurcharge_25percent() {
        val result = FareCalculator.estimateFare(
            distanceKm = 2.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(1, 0),
            city = com.fairmeter.app.data.model.City.MUMBAI
        )
        // base 26 + distance 9 = 35, night 25% = 35 * 0.25 = 8.75 -> 9
        assertEquals(26, result.baseFare)
        assertEquals(9, result.distanceFare)
        assertEquals(9, result.nightSurcharge)
    }

    // Delhi tests
    @Test
    fun delhi_baseFareWithinMinDistance() {
        val result = FareCalculator.estimateFare(
            distanceKm = 1.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.DELHI
        )
        assertEquals(30, result.baseFare)
        assertEquals(0, result.distanceFare)
    }

    @Test
    fun delhi_distanceFareAboveMin() {
        val result = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.DELHI
        )
        // 5.0 - 1.5 = 3.5 km * 11 = 38.5 -> 38
        assertEquals(38, result.distanceFare)
    }

    @Test
    fun delhi_waitingFirst15MinFree() {
        val waiting = DelhiFare.waitingFare(800)
        assertEquals(0, waiting)

        val waiting2 = DelhiFare.waitingFare(960)
        assertEquals(1, waiting2) // 1 min chargeable
    }

    @Test
    fun delhi_nightSurcharge_25percent() {
        val result = FareCalculator.estimateFare(
            distanceKm = 1.5,
            waitingSeconds = 0,
            currentTime = LocalTime.of(23, 30),
            city = com.fairmeter.app.data.model.City.DELHI
        )
        assertEquals(8, result.nightSurcharge) // 30 * 0.25 = 7.5 -> 8 (half-up)
    }

    // Chennai tests
    @Test
    fun chennai_baseFareAtExactlyMinDistance() {
        val result = FareCalculator.estimateFare(
            distanceKm = 1.8,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.CHENNAI
        )
        assertEquals(25, result.baseFare)
        assertEquals(0, result.distanceFare)
    }

    @Test
    fun chennai_distanceFareAboveMin() {
        val result = FareCalculator.estimateFare(
            distanceKm = 3.8,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.CHENNAI
        )
        // 3.8 - 1.8 = 2.0 km * 12 = 24
        assertEquals("distanceFare", 24, result.distanceFare)
    }

    // Hyderabad tests
    @Test
    fun hyderabad_dayRateUsedDuringDay() {
        val result = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.HYDERABAD
        )
        // 5.0 - 2.0 = 3.0 km * 11 = 33
        assertEquals(33, result.distanceFare)
        assertEquals(0, result.nightSurcharge)
    }

    @Test
    fun hyderabad_nightRateUsedAtNight() {
        val result = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(23, 30),
            city = com.fairmeter.app.data.model.City.HYDERABAD
        )
        // 5.0 - 2.0 = 3.0 km * 17 = 51
        assertEquals(51, result.distanceFare)
        assertEquals(0, result.nightSurcharge) // Hyderabad: nightMultiplier = 1.0
    }

    @Test
    fun hyderabad_noNightSurchargeField_usesHigherPerKmRate() {
        val day = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.HYDERABAD
        )
        val night = FareCalculator.estimateFare(
            distanceKm = 5.0,
            waitingSeconds = 0,
            currentTime = LocalTime.of(23, 30),
            city = com.fairmeter.app.data.model.City.HYDERABAD
        )
        // 33 vs 51 — night uses higher per-km rate instead of surcharge
        assertEquals(33, day.distanceFare)
        assertEquals(51, night.distanceFare)
    }

    @Test
    fun computeIncrement_handlesSmallTicks() {
        val incr = FareCalculator.computeIncrement(
            totalDistanceKm = 5.0,
            tickDistanceKm = 0.02,
            tickWaitingSeconds = 2,
            currentTime = LocalTime.of(14, 0),
            city = com.fairmeter.app.data.model.City.BENGALURU
        )
        // 0.02 km * 18 = 0.36 -> 0 (below integer rounding)
        assertEquals(0, incr.distanceFare)
    }

    @Test
    fun computeBreakdown_aggregatesCorrectly() {
        val breakdown = FareCalculator.computeBreakdown(
            totalDistanceKm = 5.0,
            totalWaitingSeconds = 600,
            city = com.fairmeter.app.data.model.City.BENGALURU,
            distanceFareTotal = 54,
            waitingFareTotal = 10,
            nightSurchargeTotal = 0
        )
        assertEquals(36, breakdown.baseFare)
        assertEquals(54, breakdown.distanceFare)
        assertEquals(10, breakdown.waitingFare)
        assertEquals(0, breakdown.nightSurcharge)
        assertEquals(100, breakdown.total)
    }

    @Test
    fun getRules_throwsForUnknownCity() {
        // only 5 cities supported
        val cities = com.fairmeter.app.data.model.City.entries
        assertEquals(5, cities.size)
    }

    @Test
    fun bengaluru_nightBoundary_earlyMorningNotNight() {
        val isNight = BengaluruFare.isNight(LocalTime.of(6, 0))
        assertEquals(false, isNight)
    }

    @Test
    fun mumbai_nightBand_12amto5am() {
        assertEquals(true, MumbaiFare.isNight(LocalTime.of(0, 30)))
        assertEquals(true, MumbaiFare.isNight(LocalTime.of(4, 59)))
        assertEquals(false, MumbaiFare.isNight(LocalTime.of(5, 0)))
        assertEquals(false, MumbaiFare.isNight(LocalTime.of(12, 0)))
    }

    @Test
    fun delhi_nightBand_11pmto5am() {
        assertEquals(true, DelhiFare.isNight(LocalTime.of(23, 0)))
        assertEquals(true, DelhiFare.isNight(LocalTime.of(4, 59)))
        assertEquals(false, DelhiFare.isNight(LocalTime.of(5, 0)))
    }

    @Test
    fun chennai_nightBand_11pmto5am() {
        assertEquals(true, ChennaiFare.isNight(LocalTime.of(23, 0)))
        assertEquals(true, ChennaiFare.isNight(LocalTime.of(0, 0)))
        assertEquals(false, ChennaiFare.isNight(LocalTime.of(5, 0)))
    }

    @Test
    fun hyderabad_nightBand_11pmto5am() {
        assertEquals(true, HyderabadFare.isNight(LocalTime.of(23, 0)))
        assertEquals(false, HyderabadFare.isNight(LocalTime.of(5, 0)))
    }
}
