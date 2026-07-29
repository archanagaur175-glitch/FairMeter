package com.fairmeter.app.data.fare

data class FareBreakdown(
    val baseFare: Int,
    val distanceFare: Int,
    val waitingFare: Int,
    val nightSurcharge: Int,
    val total: Int
)

data class FareIncrement(
    val distanceFare: Int,
    val waitingFare: Int,
    val nightSurcharge: Int,
    val totalIncrement: Int
)
