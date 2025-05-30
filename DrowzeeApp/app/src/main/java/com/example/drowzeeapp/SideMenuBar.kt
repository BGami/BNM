package com.example.drowzeeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Side menu drawer for app navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideMenuBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    driverId: String
) {
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .padding(16.dp)
        ) {
            // App logo and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Drowsiness Detection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Driver info if available
            if (driverId.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Driver ID: $driverId",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Navigation items
            NavigationItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentRoute == "home",
                onClick = {
                    onNavigate("home")
                    scope.launch { onCloseDrawer() }
                }
            )
            
            NavigationItem(
                icon = Icons.Default.Visibility,
                label = "Live Detection",
                isSelected = currentRoute == "live_prediction",
                onClick = {
                    onNavigate("live_prediction")
                    scope.launch { onCloseDrawer() }
                }
            )
            
            NavigationItem(
                icon = Icons.Default.ContactPhone,
                label = "Contact Details",
                isSelected = currentRoute == "contact_details",
                onClick = {
                    onNavigate("contact_details")
                    scope.launch { onCloseDrawer() }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Divider()
            
            // Logout option
            NavigationItem(
                icon = Icons.Default.Logout,
                label = "Logout",
                isSelected = false,
                onClick = {
                    onNavigate("login")
                    scope.launch { onCloseDrawer() }
                }
            )
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isSelected) {
        NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    } else {
        NavigationDrawerItemDefaults.colors()
    }
    
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = isSelected,
        onClick = onClick,
        colors = colors,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
