package com.fairmeter.app.ui.preTrip

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fairmeter.app.data.model.City
import com.fairmeter.app.ui.components.CityPicker
import com.fairmeter.app.ui.theme.AmberGradientEnd
import com.fairmeter.app.ui.theme.AmberGradientStart
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun PreTripScreen(
    viewModel: PreTripViewModel,
    onStartTrip: (City, Double, Double, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var sourceMarker by remember { mutableStateOf<Marker?>(null) }
    var destMarker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = "FairMeter/1.0"
        Configuration.getInstance().apply {
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("tiles")
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AmberGradientStart, AmberGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Plan Your Trip",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

            CityPicker(
                selectedCity = state.selectedCity,
                onCitySelected = { viewModel.setCity(it) }
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    val mapView = MapView(ctx)
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(12.97, 77.59))

                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                            override fun longPressHelper(p: GeoPoint): Boolean {
                                if (sourceMarker == null) {
                                    sourceMarker = Marker(mapView).apply {
                                        position = p
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Source"
                                        snippet = "${p.latitude}, ${p.longitude}"
                                        icon = ctx.getDrawable(
                                            android.R.drawable.ic_menu_mylocation
                                        )
                                    }
                                    mapView.overlays.add(sourceMarker)
                                    mapView.invalidate()
                                    viewModel.setSource(p.latitude, p.longitude)
                                } else if (destMarker == null) {
                                    destMarker = Marker(mapView).apply {
                                        position = p
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Destination"
                                        snippet = "${p.latitude}, ${p.longitude}"
                                        icon = ctx.getDrawable(
                                            android.R.drawable.ic_menu_directions
                                        )
                                    }
                                    mapView.overlays.add(destMarker)
                                    mapView.invalidate()
                                    viewModel.setDestination(p.latitude, p.longitude)
                                }
                                return true
                            }
                        })
                        mapView.overlays.add(eventsOverlay)
                    }
                    mapView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            state.estimatedFare?.let { fare ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Distance", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${"%.2f".format(state.estimatedDistanceKm)} km",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Fare", style = MaterialTheme.typography.titleLarge)
                            Text(
                                fare,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    state.sourceLat?.let { slat ->
                        state.sourceLng?.let { slng ->
                            state.destLat?.let { dlat ->
                                state.destLng?.let { dlng ->
                                    onStartTrip(state.selectedCity, slat, slng, dlat, dlng)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = state.sourceLat != null && state.destLat != null
            ) {
                Text("Start Trip", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
