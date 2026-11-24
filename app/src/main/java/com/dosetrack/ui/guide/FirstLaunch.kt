package com.dosetrack.ui.guide

/**
 * Whether to open the guide on start-up: only once, and only for someone who has nothing in the app
 * yet. People updating from an older version already have data, so they aren't interrupted.
 */
fun shouldOpenGuide(guideSeen: Boolean, medicationCount: Int, entryCount: Int): Boolean =
    !guideSeen && medicationCount == 0 && entryCount == 0
