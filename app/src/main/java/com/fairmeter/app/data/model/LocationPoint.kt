package com.fairmeter.app.data.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val bearing: Float,
    val timestamp: Long,
    val accuracy: Float
)
