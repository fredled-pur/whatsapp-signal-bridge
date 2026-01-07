package com.bridge.whatsapptosignal.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bridge.whatsapptosignal.BridgeApplication
import com.bridge.whatsapptosignal.service.WhatsAppNotificationListener
import com.bridge.whatsapptosignal.ui.screens.*
import com.bridge.whatsapptosignal.ui.theme.WhatsAppSignalBridgeTheme
import com.bridge.whatsapptosignal.ui.viewmodel.BridgeViewModel
import com.bridge.whatsapptosignal.ui.viewmodel.BridgeViewModelFactory

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as BridgeApplication
        
        setContent {
            WhatsAppSignalBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BridgeViewModel = viewModel(
                        factory = BridgeViewModelFactory(app)
                    )
                    
                    BridgeApp(
                        viewModel = viewModel,
                        onRequestNotificationAccess = { requestNotificationAccess() },
                        onRequestBatteryOptimization = { requestBatteryOptimization() }
                    )
                }
            }
        }
    }
    
    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    private fun requestBatteryOptimization() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivity(intent)
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }
}

@Composable
fun BridgeApp(
    viewModel: BridgeViewModel,
    onRequestNotificationAccess: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = if (uiState.isSetupComplete) "home" else "setup"
    ) {
        composable("setup") {
            SetupScreen(
                viewModel = viewModel,
                onRequestNotificationAccess = onRequestNotificationAccess,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
                onSetupComplete = {
                    navController.navigate("home") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToLogs = { navController.navigate("logs") }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("logs") {
            LogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
