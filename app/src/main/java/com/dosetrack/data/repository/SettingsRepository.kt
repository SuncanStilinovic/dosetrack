package com.dosetrack.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dosetrack.data.settings.AppSettings
import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.ChronoUnit

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SITE_HISTORY_COUNT = intPreferencesKey("site_history_count")
        val SHOW_GUIDE = booleanPreferencesKey("show_injection_guide")
        val REMINDER_MODE = stringPreferencesKey("reminder_mode")
        val REMINDER_DAYS = stringSetPreferencesKey("reminder_days")
        val REMINDER_TIME_MINUTES = intPreferencesKey("reminder_time_minutes")
        val REMINDER_INTERVAL_VALUE = intPreferencesKey("reminder_interval_value")
        val REMINDER_INTERVAL_UNIT = stringPreferencesKey("reminder_interval_unit")
        val REMINDER_START_AT = longPreferencesKey("reminder_start_at")
        val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
    }

    /** Stable fallback anchor so an unsaved interval start doesn't drift on every emission. */
    private val defaultStartAt: Instant = Instant.now().truncatedTo(ChronoUnit.MINUTES)

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            siteHistoryCount = prefs[Keys.SITE_HISTORY_COUNT] ?: defaults.siteHistoryCount,
            showInjectionGuide = prefs[Keys.SHOW_GUIDE] ?: defaults.showInjectionGuide,
            reminderConfig = ReminderConfig(
                mode = prefs[Keys.REMINDER_MODE]?.let { ReminderMode.valueOf(it) }
                    ?: defaults.reminderConfig.mode,
                days = prefs[Keys.REMINDER_DAYS]?.map { DayOfWeek.valueOf(it) }?.toSet()
                    ?: defaults.reminderConfig.days,
                timeOfDayMinutes = prefs[Keys.REMINDER_TIME_MINUTES]
                    ?: defaults.reminderConfig.timeOfDayMinutes,
                intervalValue = prefs[Keys.REMINDER_INTERVAL_VALUE]
                    ?: defaults.reminderConfig.intervalValue,
                intervalUnit = prefs[Keys.REMINDER_INTERVAL_UNIT]?.let { IntervalUnit.valueOf(it) }
                    ?: defaults.reminderConfig.intervalUnit,
                startAt = prefs[Keys.REMINDER_START_AT]?.let { Instant.ofEpochMilli(it) }
                    ?: defaultStartAt,
            ),
            use24HourClock = prefs[Keys.USE_24_HOUR_CLOCK],
        )
    }

    suspend fun updateSiteHistoryCount(count: Int) {
        context.dataStore.edit { it[Keys.SITE_HISTORY_COUNT] = count }
    }

    suspend fun updateShowInjectionGuide(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GUIDE] = show }
    }

    suspend fun updateUse24HourClock(use24Hour: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR_CLOCK] = use24Hour }
    }

    suspend fun updateReminderConfig(config: ReminderConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_MODE] = config.mode.name
            prefs[Keys.REMINDER_DAYS] = config.days.map { it.name }.toSet()
            prefs[Keys.REMINDER_TIME_MINUTES] = config.timeOfDayMinutes
            prefs[Keys.REMINDER_INTERVAL_VALUE] = config.intervalValue
            prefs[Keys.REMINDER_INTERVAL_UNIT] = config.intervalUnit.name
            prefs[Keys.REMINDER_START_AT] = config.startAt.toEpochMilli()
        }
    }
}
