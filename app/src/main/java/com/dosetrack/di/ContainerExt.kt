package com.dosetrack.di

import android.content.Context
import com.dosetrack.DoseTrackApplication

fun Context.appContainer(): AppContainer = (applicationContext as DoseTrackApplication).container
