package com.example.drowzeeapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home screen with project overview and feature containers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLivePrediction: () -> Unit,
    onNavigateToContactDetails: () -> Unit,
    onMenuClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top app bar with menu
        TopAppBar(
            title = { Text("Driver Drowsiness Detection") },
            navigationIcon = {
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Hero image - will be replaced with user-provided image
            Image(
                painter = painterResource(id = R.drawable.hero_image),
                contentDescription = "Driver Drowsiness Detection System",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Project overview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "About the System",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Our driver drowsiness detection system uses advanced facial landmark detection to monitor driver alertness in real-time. The system detects signs of drowsiness and fatigue to prevent accidents and save lives.",
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }
            }

            // Feature containers
            Text(
                text = "Key Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Feature 1
            FeatureContainer(
                title = "Real-time Drowsiness Detection",
                description = "Advanced facial landmark tracking monitors eye closure patterns to detect drowsiness with high accuracy.",
                icon = Icons.Default.Visibility,
                backgroundColor = Color(0xFFE3F2FD) // Light blue
            )

            // Feature 2
            FeatureContainer(
                title = "Yawn Detection",
                description = "Monitors mouth movements to detect yawning, an early indicator of fatigue and reduced alertness.",
                icon = Icons.Default.Person,
                backgroundColor = Color(0xFFE8F5E9) // Light green
            )

            // Feature 3
            FeatureContainer(
                title = "Alert System",
                description = "Provides immediate audio alerts to drivers when signs of drowsiness are detected, helping prevent accidents.",
                icon = Icons.Default.Notifications,
                backgroundColor = Color(0xFFFFF3E0) // Light orange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Button(
                onClick = onNavigateToLivePrediction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text("Start Live Detection")
            }

            OutlinedButton(
                onClick = onNavigateToContactDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Update Contact Details")
            }
        }
    }
}

@Composable
fun FeatureContainer(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
