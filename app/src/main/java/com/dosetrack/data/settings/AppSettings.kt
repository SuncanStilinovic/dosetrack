package com.dosetrack.data.settings

data class AppSettings(
    val siteHistoryCount: Int = 5,
    val showInjectionGuide: Boolean = false,
    val reminderConfig: ReminderConfig = ReminderConfig(),
)
