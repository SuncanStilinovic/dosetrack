package com.dosetrack.reminders

import java.time.Instant

/** Arms one alarm per medication. Alarms are recomputed from the saved reminders after a reboot. */
interface AlarmScheduler {
    /** Arms the alarm for [medicationId] at [at], replacing any earlier one; null cancels it. */
    fun schedule(medicationId: Long, at: Instant?)

    fun cancel(medicationId: Long)

    /** Cancels the single app-wide alarm used before reminders were per medication (up to 1.3.x). */
    fun cancelLegacy()
}
