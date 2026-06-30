package com.va3fod.localwind.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.va3fod.localwind.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    
    private val _calibrationFactor = MutableStateFlow("1.0")
    val calibrationFactor: StateFlow<String> = _calibrationFactor.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            val savedFactor = repository.calibrationFactor.first()
            _calibrationFactor.value = savedFactor.toString()
            
            val savedTheme = repository.isDarkTheme.first()
            _isDarkTheme.value = savedTheme
        }
    }

    fun updateCalibrationFactor(newValue: String) {
        _calibrationFactor.value = newValue
    }

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val factor = _calibrationFactor.value.toFloatOrNull() ?: 1.0f
            repository.saveCalibrationFactor(factor)
        }
    }
}
