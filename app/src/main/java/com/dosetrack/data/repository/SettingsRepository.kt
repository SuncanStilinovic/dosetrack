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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SITE_HISTORY_COUNT = intPreferencesKey("site_history_count")
        val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")

        /** Set once the first-launch check for opening the guide has run; device-only, not backed up. */
        val GUIDE_SEEN = booleanPreferencesKey("guide_seen")

        /** The single app-wide reminder of versions up to 1.3.x; reminders are now per medication. */
        val LEGACY_REMINDER = listOf(
            stringPreferencesKey("reminder_mode"),
            stringSetPreferencesKey("reminder_days"),
            intPreferencesKey("reminder_time_minutes"),
            intPreferencesKey("reminder_interval_value"),
            stringPreferencesKey("reminder_interval_unit"),
            longPreferencesKey("reminder_start_at"),
        )
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            siteHistoryCount = prefs[Keys.SITE_HISTORY_COUNT] ?: defaults.siteHistoryCount,
            use24HourClock = prefs[Keys.USE_24_HOUR_CLOCK],
        )
    }

    suspend fun updateSiteHistoryCount(count: Int) {
        context.dataStore.edit { it[Keys.SITE_HISTORY_COUNT] = count }
    }

    suspend fun updateUse24HourClock(use24Hour: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR_CLOCK] = use24Hour }
    }

    val guideSeen: Flow<Boolean> = context.dataStore.data.map { it[Keys.GUIDE_SEEN] ?: false }

    suspend fun markGuideSeen() {
        context.dataStore.edit { it[Keys.GUIDE_SEEN] = true }
    }

    /** Drops the old app-wide reminder setting, which is no longer used. Safe to call repeatedly. */
    suspend fun removeLegacyReminder() {
        context.dataStore.edit { prefs -> Keys.LEGACY_REMINDER.forEach { prefs.remove(it) } }
    }
}
