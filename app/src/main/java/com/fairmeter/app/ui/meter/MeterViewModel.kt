package com.fairmeter.app.ui.meter

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fairmeter.app.audio.FareAnnouncer
import com.fairmeter.app.data.model.City
import com.fairmeter.app.data.model.TripState
import com.fairmeter.app.data.tracking.TrackingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeterUiState(
    val currentFare: Int = 0,
    val distanceKm: Double = 0.0,
    val waitingSeconds: Int = 0,
    val isWaiting: Boolean = false,
    val isRunning: Boolean = false,
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0
)

class MeterViewModel(
    application: Application,
    private val fareAnnouncer: FareAnnouncer
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MeterUiState())
    val uiState: StateFlow<MeterUiState> = _uiState.asStateFlow()

    private var trackingService: TrackingService? = null
    private var tripCity: City? = null
    private var lastAnnouncedFare = 0
    private var startTime = 0L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TrackingService.LocalBinder
            trackingService = binder.getService()
            observeTripState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
        }
    }

    fun startTrip(city: City) {
        tripCity = city
        startTime = System.currentTimeMillis()
        _uiState.value = MeterUiState(isRunning = true)
        lastAnnouncedFare = 0

        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            putExtra(TrackingService.EXTRA_CITY, city.name)
        }
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            while (_uiState.value.isRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                val totalSecs = elapsed / 1000
                _uiState.value = _uiState.value.copy(
                    hours = (totalSecs / 3600).toInt(),
                    minutes = (totalSecs % 3600 / 60).toInt(),
                    seconds = (totalSecs % 60).toInt()
                )
                delay(1000)
            }
        }
    }

    private fun observeTripState() {
        viewModelScope.launch {
            trackingService?.tripState?.collect { state ->
                when (state) {
                    is TripState.Active -> {
                        _uiState.value = _uiState.value.copy(
                            currentFare = state.totalFare,
                            distanceKm = state.accumulatedDistanceKm,
                            waitingSeconds = state.accumulatedWaitingSeconds,
                            isWaiting = state.isWaiting
                        )
                        val fare = state.totalFare
                        if (fare - lastAnnouncedFare >= 10) {
                            fareAnnouncer.speakFare(fare)
                            lastAnnouncedFare = fare
                        }
                    }
                    is TripState.Ended -> {
                        _uiState.value = _uiState.value.copy(isRunning = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun stopTrip() {
        _uiState.value = _uiState.value.copy(isRunning = false)
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
        trackingService?.endTrip()
        trackingService = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
    }
}
