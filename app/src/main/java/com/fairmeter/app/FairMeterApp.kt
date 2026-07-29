package com.fairmeter.app

import android.app.Application
import android.content.Context
import com.fairmeter.app.audio.FareAnnouncer
import com.fairmeter.app.data.fare.FareCalculator

class FairMeterApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val fareCalculator = FareCalculator
    val fareAnnouncer = FareAnnouncer(context)
}
