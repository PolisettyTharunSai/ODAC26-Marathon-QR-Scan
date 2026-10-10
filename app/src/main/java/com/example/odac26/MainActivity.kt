package com.example.odac26

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.odac26.repository.MarathonRepository
import com.example.odac26.ui.AdminUploadScreen
import com.example.odac26.ui.AttendanceScreen
import com.example.odac26.ui.DashboardScreen
import com.example.odac26.ui.FinishScannerScreen
import com.example.odac26.ui.RaceStartScreen
import com.example.odac26.ui.theme.ODAC26Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ODAC26Theme {
                MarathonApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarathonApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val repository = remember { MarathonRepository() }
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var isCheckingPassword by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun handleAdminNavigation(route: String) {
        showMenu = false
        if (isAdminAuthenticated) {
            navController.navigate(route)
        } else {
            pendingRoute = route
            passwordInput = ""
            passwordError = ""
            passwordVisible = false
            showPasswordDialog = true
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                pendingRoute = null
            },
            title = { Text("Admin Password Required") },
            text = {
                Column {
                    Text("Please enter the admin password to perform admin operations.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = ""
                        },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        isError = passwordError.isNotEmpty(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.isBlank()) {
                            passwordError = "Password cannot be empty"
                            return@Button
                        }
                        isCheckingPassword = true
                        scope.launch {
                            val isValid = repository.verifyAdminPassword(passwordInput)
                            isCheckingPassword = false
                            if (isValid) {
                                isAdminAuthenticated = true
                                showPasswordDialog = false
                                pendingRoute?.let { route ->
                                    navController.navigate(route)
                                    pendingRoute = null
                                }
                            } else {
                                passwordError = "Incorrect password"
                            }
                        }
                    },
                    enabled = !isCheckingPassword
                ) {
                    if (isCheckingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Submit")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        pendingRoute = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ODAC26 Marathon") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Admin Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Admin Upload (CSV)") },
                            onClick = { handleAdminNavigation("upload") }
                        )
                        DropdownMenuItem(
                            text = { Text("Trigger Race Start") },
                            onClick = { handleAdminNavigation("start") }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            // Only show bottom bar on the main 3 tabs
            if (currentRoute in listOf("attendance", "home", "dashboard")) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Attendance") },
                        label = { Text("Attendance") },
                        selected = currentRoute == "attendance",
                        onClick = {
                            navController.navigate("attendance") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController, 
                startDestination = "home",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable("attendance") {
                    AttendanceScreen()
                }
                composable("home") {
                    FinishScannerScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToUpload = {}, // No longer used from dashboard
                        onNavigateToAttendance = {},
                        onNavigateToRaceStart = {},
                        onNavigateToScanner = {}
                    )
                }
                composable("upload") {
                    AdminUploadScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("start") {
                    RaceStartScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}