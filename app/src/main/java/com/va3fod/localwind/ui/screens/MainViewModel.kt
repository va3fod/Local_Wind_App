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
    val lockedWindSpeed: Double = 0.0,
    val lockedAltitude: Double = 0.0,
    val lockedDeclination: Float = 0f,
    val calibrationFactor: Float = 1.0f,
    val showTrueNorth: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val sensorProvider = SensorProvider(application)
    private val windAnalyzer = WindAnalyzer()
    private val locationProvider = LocationProvider(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
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

        settingsRepository.calibrationFactor
            .onEach { factor ->
                _uiState.update { it.copy(calibrationFactor = factor) }
            }
            .launchIn(viewModelScope)
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
            
            val rawSpeed = windAnalyzer.measureWindSpeed()
            val calibratedSpeed = rawSpeed * _uiState.value.calibrationFactor

            val declination = telemetry?.declination ?: 0f
            
            // Formula: Wind Origin = (Capture Heading + Declination + 180) mod 360
            // Reverting to the version that includes the 180-degree offset for back-camera capture.
            val trueWindOrigin = (captureAzimuth + declination + 180f + 360f) % 360f

            _uiState.update { 
                it.copy(
                    screenState = MainScreenState.VISUALIZATION,
                    lockedWindSpeed = calibratedSpeed,
                    lockedWindDirection = trueWindOrigin,
                    lockedAltitude = telemetry?.altitude ?: 0.0,
                    lockedDeclination = declination
                )
            }
        }
    }

    fun toggleNorthType() {
        _uiState.update { it.copy(showTrueNorth = !it.showTrueNorth) }
    }
}
