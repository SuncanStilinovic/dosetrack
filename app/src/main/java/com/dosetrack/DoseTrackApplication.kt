package com.dosetrack

import android.app.Application
import com.dosetrack.di.AppContainer
import com.dosetrack.di.DefaultAppContainer

class DoseTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
