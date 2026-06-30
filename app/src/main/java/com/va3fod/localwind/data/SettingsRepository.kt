package com.va3fod.localwind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val calibrationFactorKey = floatPreferencesKey("calibration_factor")
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val smoothingWindowKey = intPreferencesKey("smoothing_window")

    val calibrationFactor: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[calibrationFactorKey] ?: 1.0f
        }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[isDarkThemeKey] ?: true
        }

    val smoothingWindow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[smoothingWindowKey] ?: 60
        }

    suspend fun saveCalibrationFactor(factor: Float) {
        context.dataStore.edit { preferences ->
            preferences[calibrationFactorKey] = factor
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isDarkThemeKey] = isDark
        }
    }

    suspend fun saveSmoothingWindow(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[smoothingWindowKey] = size
        }
    }
}
