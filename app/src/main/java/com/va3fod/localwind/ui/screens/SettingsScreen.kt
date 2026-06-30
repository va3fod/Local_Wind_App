package com.va3fod.localwind.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val calibrationFactor by viewModel.calibrationFactor.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    Surface(color = backgroundColor, contentColor = contentColor) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = contentColor,
                        navigationIconContentColor = contentColor
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Calibration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tune the acoustic algorithm. The multiplier is applied immediately to the raw RMS values during wind speed calculation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = calibrationFactor,
                    onValueChange = { viewModel.updateCalibrationFactor(it) },
                    label = { Text("Manual Calibration Multiplier") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = contentColor,
                        unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
                        focusedLabelColor = contentColor,
                        unfocusedLabelColor = contentColor.copy(alpha = 0.5f),
                        cursorColor = contentColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Default is 1.0. Higher values increase reported wind speed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Additional Professional Settings
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.setTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { 
                        viewModel.saveSettings()
                        scope.launch {
                            snackbarHostState.showSnackbar("Settings saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color.White else Color.Black,
                        contentColor = if (isDarkTheme) Color.Black else Color.White
                    )
                ) {
                    Text("SAVE SETTINGS")
                }
            }
        }
    }
}
