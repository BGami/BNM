package com.example.drowzeeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DrowsinessDetectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationWithDrawer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationWithDrawer() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State for driver ID and supervisor mobile
    var driverId by remember { mutableStateOf("") }
    var supervisorMobile by remember { mutableStateOf("") }

    // Get current route
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "login"

    // Only show drawer when logged in (not on login screen)
    val showDrawer = currentRoute != "login"

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SideMenuBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    driverId = driverId
                )
            },
            content = {
                AppNavigationContent(
                    navController = navController,
                    driverId = driverId,
                    supervisorMobile = supervisorMobile,
                    onDriverInfoUpdated = { id, mobile ->
                        driverId = id
                        supervisorMobile = mobile
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        )
    } else {
        // Just show the content without drawer for login screen
        AppNavigationContent(
            navController = navController,
            driverId = driverId,
            supervisorMobile = supervisorMobile,
            onDriverInfoUpdated = { id, mobile ->
                driverId = id
                supervisorMobile = mobile
            },
            onMenuClick = {}
        )
    }
}

@Composable
fun AppNavigationContent(
    navController: NavHostController,
    driverId: String,
    supervisorMobile: String,
    onDriverInfoUpdated: (String, String) -> Unit,
    onMenuClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Clear the back stack so login isn't in history
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToLivePrediction = {
                    if (driverId.isBlank() || supervisorMobile.isBlank()) {
                        navController.navigate("contact_details")
                    } else {
                        navController.navigate("live_prediction")
                    }
                },
                onNavigateToContactDetails = {
                    navController.navigate("contact_details")
                },
                onMenuClick = onMenuClick
            )
        }

        composable("contact_details") {
            ContactDetailsScreen(
                onSaveDetails = { id, mobile ->
                    onDriverInfoUpdated(id, mobile)
                    navController.navigate("home")
                },
                onBack = {
                    navController.navigateUp()
                },
                onMenuClick = onMenuClick
            )
        }

        composable("live_prediction") {
            LivePredictionScreen(
                driverId = driverId,
                supervisorMobile = supervisorMobile,
                onBack = {
                    // Always navigate to home when back is pressed from live prediction
                    navController.navigate("home") {
                        // Pop up to home to avoid building up back stack
                        popUpTo("home") { inclusive = false }
                    }
                },
                onMenuClick = onMenuClick
            )
        }
    }
}
