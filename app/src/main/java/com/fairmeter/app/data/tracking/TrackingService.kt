package com.fairmeter.app.data.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.fairmeter.app.R
import com.fairmeter.app.data.fare.FareCalculator
import com.fairmeter.app.data.model.City
import com.fairmeter.app.data.model.TripState
import com.fairmeter.app.domain.Haversine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

class TrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private lateinit var waitingDetector: WaitingDetector

    private val pathSmoother = PathSmoother()
    private var lastSmoothedPoint: com.fairmeter.app.data.model.LocationPoint? = null
    private val accumulatedDistanceMeters = mutableListOf<Double>()
    private var accumulatedWaitingSeconds = 0
    private var lastWaitingState = false
    private var waitingTickStart = 0L

    // Accumulated fare totals (tick-based)
    private var accumulatedDistanceFare = 0
    private var accumulatedWaitingFare = 0
    private var accumulatedNightSurcharge = 0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        waitingDetector = WaitingDetector(sensorManager)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cityName = intent?.getStringExtra(EXTRA_CITY) ?: return START_NOT_STICKY
        val city = City.valueOf(cityName)

        _tripState.value = TripState.Active(
            city = city,
            startTime = System.currentTimeMillis()
        )

        pathSmoother.reset()
        waitingDetector.start()
        accumulatedDistanceMeters.clear()
        accumulatedWaitingSeconds = 0
        lastWaitingState = false
        waitingTickStart = 0L
        accumulatedDistanceFare = 0
        accumulatedWaitingFare = 0
        accumulatedNightSurcharge = 0

        startLocationUpdates(city)
        return START_STICKY
    }

    private fun startLocationUpdates(city: City) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val rawPoint = com.fairmeter.app.data.model.LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = location.speed,
                        bearing = location.bearing,
                        timestamp = location.time,
                        accuracy = location.accuracy
                    )

                    val smoothed = pathSmoother.smooth(rawPoint) ?: continue
                    waitingDetector.onSpeedUpdate(smoothed.speed)

                    val prev = lastSmoothedPoint
                    if (prev != null && !_tripState.value.isWaiting()) {
                        val deltaKm = Haversine.distance(
                            prev.latitude, prev.longitude,
                            smoothed.latitude, smoothed.longitude
                        )
                        accumulatedDistanceMeters.add(deltaKm * 1000)

                        val totalKm = accumulatedDistanceMeters.sum() / 1000.0
                        val tickDistanceKm = deltaKm

                        val currentState = _tripState.value as? TripState.Active ?: return

                        val isWaitingNow = waitingDetector.waitingState.value
                        if (isWaitingNow) {
                            if (!lastWaitingState) {
                                waitingTickStart = System.currentTimeMillis()
                            }
                            val waitDuration = (System.currentTimeMillis() - waitingTickStart).toInt() / 1000
                            accumulatedWaitingSeconds = waitDuration

                            if (lastWaitingState) {
                                val waitIncrement = 2
                                accumulatedWaitingSeconds += waitIncrement
                            }
                        } else {
                            if (lastWaitingState) {
                                waitingTickStart = 0L
                            }
                        }
                        lastWaitingState = isWaitingNow

                        val increment = FareCalculator.computeIncrement(
                            totalDistanceKm = totalKm,
                            tickDistanceKm = tickDistanceKm,
                            tickWaitingSeconds = if (isWaitingNow) 2 else 0,
                            currentTime = LocalTime.now(),
                            city = city
                        )

                        accumulatedDistanceFare += increment.distanceFare
                        accumulatedWaitingFare += increment.waitingFare
                        accumulatedNightSurcharge += increment.nightSurcharge

                        _tripState.value = currentState.copy(
                            accumulatedDistanceKm = totalKm,
                            accumulatedWaitingSeconds = accumulatedWaitingSeconds,
                            totalFare = accumulatedDistanceFare + accumulatedWaitingFare + accumulatedNightSurcharge,
                            isWaiting = isWaitingNow
                        )
                    }
                    lastSmoothedPoint = smoothed
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun endTrip() {
        val current = _tripState.value
        if (current is TripState.Active) {
            val breakdown = FareCalculator.computeBreakdown(
                totalDistanceKm = current.accumulatedDistanceKm,
                totalWaitingSeconds = current.accumulatedWaitingSeconds,
                city = current.city,
                distanceFareTotal = accumulatedDistanceFare,
                waitingFareTotal = accumulatedWaitingFare,
                nightSurchargeTotal = accumulatedNightSurcharge
            )

            _tripState.value = TripState.Ended(
                city = current.city,
                startTime = current.startTime,
                endTime = System.currentTimeMillis(),
                totalDistanceKm = current.accumulatedDistanceKm,
                totalWaitingSeconds = current.accumulatedWaitingSeconds,
                baseFare = breakdown.baseFare,
                distanceFare = breakdown.distanceFare,
                waitingFare = breakdown.waitingFare,
                nightSurcharge = breakdown.nightSurcharge,
                totalFare = breakdown.total
            )
        }
        stopLocationUpdates()
        waitingDetector.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {})
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        waitingDetector.stop()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    private var _tripState = MutableStateFlow<TripState>(TripState.Idle)
    val tripState: StateFlow<TripState> = _tripState.asStateFlow()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trip Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FairMeter")
            .setContentText("Trip in progress")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_CITY = "extra_city"
        private const val CHANNEL_ID = "fairmeter_tracking"
        private const val NOTIFICATION_ID = 1
    }
}

private fun TripState.isWaiting(): Boolean {
    return when (this) {
        is TripState.Active -> isWaiting
        else -> false
    }
}
