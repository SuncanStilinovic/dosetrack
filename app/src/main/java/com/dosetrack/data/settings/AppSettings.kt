package com.dosetrack.data.settings

data class AppSettings(
    val siteHistoryCount: Int = 3,
    val reminderConfig: ReminderConfig = ReminderConfig(),
    /** True for a 24-hour clock, false for 12-hour; null until the user picks one (follow the device). */
    val use24HourClock: Boolean? = null,
) {
    companion object {
        val SITE_HISTORY_RANGE = 0..100
    }
}
