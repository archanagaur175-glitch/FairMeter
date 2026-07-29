package com.fairmeter.app.ui.preTrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fairmeter.app.data.fare.FareCalculator
import com.fairmeter.app.data.model.City
import com.fairmeter.app.domain.EstimateRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

data class PreTripUiState(
    val selectedCity: City = City.BENGALURU,
    val sourceLat: Double? = null,
    val sourceLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val estimatedDistanceKm: Double? = null,
    val estimatedFare: String? = null,
    val cityList: List<City> = City.entries
)

class PreTripViewModel(
    private val fareCalculator: FareCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreTripUiState())
    val uiState: StateFlow<PreTripUiState> = _uiState.asStateFlow()

    fun setCity(city: City) {
        _uiState.value = _uiState.value.copy(selectedCity = city)
        recalculate()
    }

    fun setSource(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(sourceLat = lat, sourceLng = lng)
        recalculate()
    }

    fun setDestination(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(destLat = lat, destLng = lng)
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val srcLat = state.sourceLat ?: return
        val srcLng = state.sourceLng ?: return
        val dstLat = state.destLat ?: return
        val dstLng = state.destLng ?: return

        viewModelScope.launch {
            val distance = EstimateRoute.estimatedRouteDistance(srcLat, srcLng, dstLat, dstLng)
            val breakdown = fareCalculator.estimateFare(
                distanceKm = distance,
                waitingSeconds = 0,
                currentTime = LocalTime.now(),
                city = state.selectedCity
            )
            _uiState.value = state.copy(
                estimatedDistanceKm = distance,
                estimatedFare = "₹${breakdown.total}"
            )
        }
    }

    class Factory(
        private val fareCalculator: FareCalculator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PreTripViewModel(fareCalculator) as T
        }
    }
}
