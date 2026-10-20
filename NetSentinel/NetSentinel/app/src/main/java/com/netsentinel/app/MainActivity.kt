package com.netsentinel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.netsentinel.app.navigation.Destination
import com.netsentinel.app.ui.capture.PacketCaptureScreen
import com.netsentinel.app.ui.control.DeviceControlScreen
import com.netsentinel.app.ui.dashboard.DashboardScreen
import com.netsentinel.app.ui.inventory.DeviceDetailScreen
import com.netsentinel.app.ui.inventory.InventoryScreen
import com.netsentinel.app.ui.mitm.MitmLabScreen
import com.netsentinel.app.ui.privacy.PrivacyScreen
import com.netsentinel.app.ui.reports.SecurityReportsScreen
import com.netsentinel.app.ui.scanner.ScannerScreen
import com.netsentinel.app.ui.theme.NetSentinelTheme
import com.netsentinel.app.ui.wifi.WifiAnalyzerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetSentinelTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NetSentinelNavHost(rememberNavController())
                }
            }
        }
    }
}

@Composable
fun NetSentinelNavHost(navController: NavHostController) {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NetSentinelApp)

    NavHost(navController = navController, startDestination = Destination.Dashboard.route) {
        composable(Destination.Dashboard.route) {
            DashboardScreen(app = app, onNavigate = { navController.navigate(it) })
        }
        composable(Destination.WifiAnalyzer.route) {
            WifiAnalyzerScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.Scanner.route) {
            ScannerScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.Inventory.route) {
            InventoryScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onOpenDevice = { ip -> navController.navigate(Destination.DeviceDetail.route(ip)) },
            )
        }
        composable(
            Destination.DeviceDetail.route,
            arguments = listOf(navArgument("ip") { type = NavType.StringType })
        ) { backStackEntry ->
            val ipArg = backStackEntry.arguments?.getString("ip").orEmpty()
            DeviceDetailScreen(app = app, ipAddress = ipArg, onBack = { navController.popBackStack() })
        }
        composable(Destination.Capture.route) {
            PacketCaptureScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.MitmLab.route) {
            MitmLabScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.Reports.route) {
            SecurityReportsScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.Privacy.route) {
            PrivacyScreen(app = app, onBack = { navController.popBackStack() })
        }
        composable(Destination.DeviceControl.route) {
            DeviceControlScreen(app = app, onBack = { navController.popBackStack() })
        }
    }
}
