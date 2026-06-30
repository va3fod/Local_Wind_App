package com.va3fod.localwind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
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
import com.va3fod.localwind.ui.components.SupportDialog

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
    
    // Theme Colors based on state
    val backgroundColor = if (uiState.isDarkTheme) Color.Black else Color.White
    val contentColor = if (uiState.isDarkTheme) Color.White else Color.Black
    val surfaceColor = if (uiState.isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)

    if (uiState.showSupportDialog) {
        SupportDialog(onDismiss = { viewModel.dismissSupport() })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("LocalWind", fontWeight = FontWeight.Bold)
                            Text("by VA3FOD", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showSupport() }) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = "Support")
                        }
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = contentColor,
                        actionIconContentColor = contentColor
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
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isDarkTheme) Color.White else Color.Black,
                                contentColor = if (uiState.isDarkTheme) Color.Black else Color.White
                            )
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
                            lineHeight = 32.sp,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (uiState.isSteady) {
                            Text("READY TO CAPTURE", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Point back camera into wind", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Stabilizing device...", color = Color.Red)
                            Text("Hold phone upright (Portrait)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.weight(1.5f))
                    }

                    MainScreenState.ACQUISITION -> {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Measuring...", style = MaterialTheme.typography.headlineMedium, color = contentColor)
                        Spacer(modifier = Modifier.height(32.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = contentColor)
                        Spacer(modifier = Modifier.weight(1.5f))
                    }

                    MainScreenState.VISUALIZATION -> {
                        // Header Bar - CORRECTED THEME CONTRAST
                        // In Dark Theme: Black background, White text
                        // In Light Theme: White background, Black text
                        val trueHeading = (uiState.currentAzimuth + uiState.lockedDeclination + 360) % 360
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor) // Changed from contentColor to backgroundColor
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${trueHeading.toInt()}° ${getDirectionString(trueHeading)}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = contentColor, // Changed from backgroundColor to contentColor
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CompassDashboard(
                            currentAzimuth = uiState.currentAzimuth,
                            lockedWindDirection = uiState.lockedWindDirection,
                            declination = uiState.lockedDeclination,
                            isDarkTheme = uiState.isDarkTheme,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                TelemetryRow("Wind Speed", "%.1f km/h".format(uiState.calibratedWindSpeed), contentColor)
                                TelemetryRow("Direction", "%.0f° True".format(uiState.lockedWindDirection), contentColor)
                                TelemetryRow("Altitude", "%.0f m".format(uiState.lockedAltitude), contentColor)
                                TelemetryRow("Declination", "%.1f°".format(uiState.lockedDeclination), contentColor)
                            }
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
fun TelemetryRow(label: String, value: String, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = if (textColor == Color.White) Color.Cyan else Color(0xFF0056b3))
    }
}
