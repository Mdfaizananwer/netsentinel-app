package com.netsentinel.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.navigation.Destination
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.StatusBadge
import com.netsentinel.app.ui.common.StatusLevel
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

private data class DashboardCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val route: String,
)

@Composable
fun DashboardScreen(app: NetSentinelApp, onNavigate: (String) -> Unit) {
    val viewModel: DashboardViewModel = composeViewModel(factory = viewModelFactory {
        initializer { DashboardViewModel(app) }
    })
    val snapshot by viewModel.snapshot.collectAsState()
    val devices by viewModel.deviceCount.collectAsState(initial = emptyList())

    val cards = listOf(
        DashboardCard("Wi-Fi Status", snapshot?.let { if (it.isWifiConnected) "Connected" else "Not connected" } ?: "Checking…", Icons.Filled.Wifi, NeonCyan, Destination.WifiAnalyzer.route),
        DashboardCard("Network Information", snapshot?.ssid ?: "View details", Icons.Filled.Router, NeonCyan, Destination.WifiAnalyzer.route),
        DashboardCard("Network Scanner", "Discover devices on your subnet", Icons.Filled.Radar, NeonGreen, Destination.Scanner.route),
        DashboardCard("Device Inventory", "${devices.size} known device(s)", Icons.Filled.List, NeonGreen, Destination.Inventory.route),
        DashboardCard("Packet Capture Lab", "On-device VPN metadata capture", Icons.Filled.NetworkCheck, NeonPurple, Destination.Capture.route),
        DashboardCard("MITM Security Lab", "Learn ARP/DNS spoofing & defenses", Icons.Filled.Shield, NeonPurple, Destination.MitmLab.route),
        DashboardCard("Security Reports", "Session summaries & findings", Icons.Filled.Security, NeonCyan, Destination.Reports.route),
        DashboardCard("Device Control", "Paired, authenticated device commands", Icons.Filled.Devices, NeonGreen, Destination.DeviceControl.route),
        DashboardCard("Privacy & Data", "What's stored, and deletion controls", Icons.Filled.PrivacyTip, NeonPurple, Destination.Privacy.route),
    )

    Scaffold(topBar = {
        TopAppBar(title = { Text("NetSentinel", style = MaterialTheme.typography.headlineSmall) })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Status", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(
                            if (snapshot?.isWifiConnected == true) "Wi-Fi connected" else "Wi-Fi not connected",
                            if (snapshot?.isWifiConnected == true) StatusLevel.OK else StatusLevel.WARNING,
                        )
                        snapshot?.let { LabelValueRow("Last checked", formatTimestamp(it.capturedAtMillis)) }
                        snapshot?.ssidUnavailableReason?.let {
                            StatusBadge("SSID hidden: $it", StatusLevel.WARNING)
                        }
                    }
                }
            }
            items(cards) { card ->
                NeonCard(
                    accentColor = card.accent,
                    modifier = Modifier.clickableCard { onNavigate(card.route) },
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(card.icon, contentDescription = null, tint = card.accent)
                        Column {
                            Text(card.title, style = MaterialTheme.typography.titleMedium)
                            Text(card.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableCard(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
