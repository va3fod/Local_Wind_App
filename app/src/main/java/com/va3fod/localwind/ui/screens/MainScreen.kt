package com.va3fod.localwind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.va3fod.localwind.ui.components.CompassDashboard

enum class MainScreenState {
    IDLE,
    ACQUISITION,
    VISUALIZATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LocalWind", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (uiState.screenState != MainScreenState.ACQUISITION) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { 
                            if (uiState.screenState == MainScreenState.IDLE) {
                                viewModel.proceedToAcquisition()
                            } else {
                                viewModel.startMeasurementFlow()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = uiState.screenState != MainScreenState.IDLE || uiState.isSteady,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            when (uiState.screenState) {
                                MainScreenState.IDLE -> "CAPTURE NOW"
                                else -> "NEW MEASUREMENT"
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.screenState) {
                MainScreenState.IDLE -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Hold phone vertically,\nback camera pointing into the wind.",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (uiState.isSteady) {
                        Text("READY TO CAPTURE", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Point back camera into wind", style = MaterialTheme.typography.bodySmall)
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stabilizing device...", color = Color.Red)
                        Text("Hold phone upright (Portrait)", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.weight(1.5f))
                }

                MainScreenState.ACQUISITION -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Measuring...", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(32.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.weight(1.5f))
                }

                MainScreenState.VISUALIZATION -> {
                    // Header Bar - Always shows True Heading
                    val trueHeading = (uiState.currentAzimuth + uiState.lockedDeclination + 360) % 360
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${trueHeading.toInt()}° ${getDirectionString(trueHeading)}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CompassDashboard(
                        currentAzimuth = uiState.currentAzimuth,
                        lockedWindDirection = uiState.lockedWindDirection,
                        declination = uiState.lockedDeclination,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TelemetryRow("Wind Speed", "%.1f km/h".format(uiState.lockedWindSpeed))
                            TelemetryRow("Direction", "%.0f° True".format(uiState.lockedWindDirection))
                            TelemetryRow("Altitude", "%.0f m".format(uiState.lockedAltitude))
                            TelemetryRow("Declination", "%.1f°".format(uiState.lockedDeclination))
                        }
                    }
                }
            }
        }
    }
}

fun getDirectionString(azimuth: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    val index = (((azimuth + 22.5) % 360) / 45).toInt()
    return directions[index]
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}
