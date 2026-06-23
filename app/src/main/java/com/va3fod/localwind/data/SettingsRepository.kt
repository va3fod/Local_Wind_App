package com.va3fod.localwind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val calibrationFactorKey = floatPreferencesKey("calibration_factor")

    val calibrationFactor: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[calibrationFactorKey] ?: 1.0f
        }

    suspend fun saveCalibrationFactor(factor: Float) {
        context.dataStore.edit { preferences ->
            preferences[calibrationFactorKey] = factor
        }
    }
}
