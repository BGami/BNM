package com.example.drowzeeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Contact details screen for driver ID and supervisor mobile number
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    onSaveDetails: (driverId: String, supervisorMobile: String) -> Unit,
    onBack: () -> Unit,
    onMenuClick: () -> Unit
) {
    var driverId by remember { mutableStateOf("") }
    var supervisorMobile by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    // Validate form whenever inputs change
    LaunchedEffect(driverId, supervisorMobile) {
        isFormValid = driverId.isNotBlank() &&
                supervisorMobile.isNotBlank() &&
                supervisorMobile.length >= 10
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Contact Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Driver Information",
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = driverId,
                onValueChange = { driverId = it },
                label = { Text("Driver ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = supervisorMobile,
                onValueChange = { supervisorMobile = it },
                label = { Text("Supervisor Mobile Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Text(
                text = "This information will be used to send alerts and reports in case of drowsiness detection.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { onSaveDetails(driverId, supervisorMobile) },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save and Continue")
            }
        }
    }
}
