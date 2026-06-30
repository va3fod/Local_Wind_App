package com.va3fod.localwind.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.va3fod.localwind.data.SettingsRepository
import com.va3fod.localwind.logic.LocationProvider
import com.va3fod.localwind.logic.SensorProvider
import com.va3fod.localwind.logic.WindAnalyzer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val screenState: MainScreenState = MainScreenState.IDLE,
    val currentAzimuth: Float = 0f,
    val isSteady: Boolean = false,
    val lockedWindDirection: Float = 0f,
    val rawLockedWindSpeed: Double = 0.0, // Store raw speed for dynamic calibration
    val lockedAltitude: Double = 0.0,
    val lockedDeclination: Float = 0f,
    val calibrationFactor: Float = 1.0f,
    val isDarkTheme: Boolean = true,
    val showSupportDialog: Boolean = false
) {
    // Dynamically calculate speed based on current calibration factor
    val calibratedWindSpeed: Double
        get() = rawLockedWindSpeed * calibrationFactor
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val sensorProvider = SensorProvider(application)
    private val windAnalyzer = WindAnalyzer()
    private val locationProvider = LocationProvider(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Observe sensors
        sensorProvider.getSensorUpdates()
            .onEach { sensorData ->
                _uiState.update { 
                    it.copy(
                        currentAzimuth = sensorData.azimuth,
                        isSteady = sensorData.isSteady
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe Settings
        settingsRepository.calibrationFactor
            .onEach { factor -> _uiState.update { it.copy(calibrationFactor = factor) } }
            .launchIn(viewModelScope)

        settingsRepository.isDarkTheme
            .onEach { isDark -> _uiState.update { it.copy(isDarkTheme = isDark) } }
            .launchIn(viewModelScope)
    }

    fun toggleTheme() {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    fun showSupport() {
        _uiState.update { it.copy(showSupportDialog = true) }
    }

    fun dismissSupport() {
        _uiState.update { it.copy(showSupportDialog = false) }
    }

    fun startMeasurementFlow() {
        _uiState.update { it.copy(screenState = MainScreenState.IDLE) }
    }

    fun proceedToAcquisition() {
        if (!_uiState.value.isSteady) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = MainScreenState.ACQUISITION) }

            val telemetry = locationProvider.getCurrentTelemetry()
            val captureAzimuth = _uiState.value.currentAzimuth
            
            // Capture raw wind speed
            val rawSpeed = windAnalyzer.measureWindSpeed()

            val declination = telemetry?.declination ?: 0f
            val trueWindOrigin = (captureAzimuth + declination + 180f + 360f) % 360f

            _uiState.update { 
                it.copy(
                    screenState = MainScreenState.VISUALIZATION,
                    rawLockedWindSpeed = rawSpeed,
                    lockedWindDirection = trueWindOrigin,
                    lockedAltitude = telemetry?.altitude ?: 0.0,
                    lockedDeclination = declination
                )
            }
        }
    }
}
