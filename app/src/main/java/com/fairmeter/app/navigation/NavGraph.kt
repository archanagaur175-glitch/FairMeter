package com.fairmeter.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fairmeter.app.FairMeterApp
import com.fairmeter.app.data.model.City
import com.fairmeter.app.data.model.TripState
import com.fairmeter.app.ui.incident.IncidentScreen
import com.fairmeter.app.ui.incident.IncidentViewModel
import com.fairmeter.app.ui.meter.MeterScreen
import com.fairmeter.app.ui.meter.MeterViewModel
import com.fairmeter.app.ui.preTrip.PreTripScreen
import com.fairmeter.app.ui.preTrip.PreTripViewModel
import com.fairmeter.app.ui.settings.SettingsScreen
import com.fairmeter.app.ui.splash.SplashScreen
import com.fairmeter.app.ui.summary.SummaryScreen


object Routes {
    const val SPLASH = "splash"
    const val PRE_TRIP = "pre_trip"
    const val METER = "meter/{city}"
    const val SUMMARY = "summary/{baseFare}/{distanceFare}/{waitingFare}/{nightSurcharge}/{totalFare}/{distanceKm}/{waitingSeconds}/{city}/{startTime}/{endTime}"
    const val INCIDENT = "incident"
    const val SETTINGS = "settings"
}

@Composable
fun FairMeterNavHost(
    navController: NavHostController,
    app: FairMeterApp,
    onRequestLocationPermission: (onGranted: () -> Unit) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onGetStarted = {
                    navController.navigate(Routes.PRE_TRIP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PRE_TRIP) {
            val viewModel: PreTripViewModel = viewModel(
                factory = PreTripViewModel.Factory(app.appContainer.fareCalculator)
            )
            PreTripScreen(
                viewModel = viewModel,
                onStartTrip = { city, slat, slng, dlat, dlng ->
                    onRequestLocationPermission {
                        navController.navigate("meter/${city.name}")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.METER,
            arguments = listOf(navArgument("city") { type = NavType.StringType })
        ) { backStackEntry ->
            val cityName = backStackEntry.arguments?.getString("city") ?: City.BENGALURU.name
            val city = City.valueOf(cityName)
            val viewModel: MeterViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return MeterViewModel(app, app.appContainer.fareAnnouncer) as T
                    }
                }
            )
            viewModel.startTrip(city)

            MeterScreen(
                viewModel = viewModel,
                onIncident = {
                    navController.navigate(Routes.INCIDENT)
                },
                onTripEnded = {
                    // Navigate to summary after trip ends
                    navController.navigate("summary/0/0/0/0/0/0.0/0/${city.name}/0/0") {
                        popUpTo(Routes.SPLASH)
                    }
                }
            )
        }

        composable(
            route = Routes.SUMMARY,
            arguments = listOf(
                navArgument("baseFare") { type = NavType.IntType; defaultValue = 0 },
                navArgument("distanceFare") { type = NavType.IntType; defaultValue = 0 },
                navArgument("waitingFare") { type = NavType.IntType; defaultValue = 0 },
                navArgument("nightSurcharge") { type = NavType.IntType; defaultValue = 0 },
                navArgument("totalFare") { type = NavType.IntType; defaultValue = 0 },
                navArgument("distanceKm") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("waitingSeconds") { type = NavType.IntType; defaultValue = 0 },
                navArgument("city") { type = NavType.StringType },
                navArgument("startTime") { type = NavType.LongType; defaultValue = 0L },
                navArgument("endTime") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val city = City.valueOf(args?.getString("city") ?: City.BENGALURU.name)
            val ended = TripState.Ended(
                city = city,
                startTime = args?.getLong("startTime") ?: 0L,
                endTime = args?.getLong("endTime") ?: 0L,
                totalDistanceKm = (args?.getFloat("distanceKm") ?: 0f).toDouble(),
                totalWaitingSeconds = args?.getInt("waitingSeconds") ?: 0,
                baseFare = args?.getInt("baseFare") ?: 0,
                distanceFare = args?.getInt("distanceFare") ?: 0,
                waitingFare = args?.getInt("waitingFare") ?: 0,
                nightSurcharge = args?.getInt("nightSurcharge") ?: 0,
                totalFare = args?.getInt("totalFare") ?: 0
            )
            SummaryScreen(
                tripData = ended,
                onNewTrip = {
                    navController.navigate(Routes.PRE_TRIP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INCIDENT) {
            val viewModel: IncidentViewModel = viewModel()
            IncidentScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                isTtsEnabled = true,
                onTtsToggle = { app.appContainer.fareAnnouncer.setEnabled(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
