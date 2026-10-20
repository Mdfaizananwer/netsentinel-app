@file:OptIn(ExperimentalMaterial3Api::class)

package com.netsentinel.app.ui.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.StatusBadge
import com.netsentinel.app.ui.common.StatusLevel
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.common.rememberPermissionState
import com.netsentinel.app.ui.common.locationPermissions
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

@Composable
fun WifiAnalyzerScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val viewModel: WifiAnalyzerViewModel = composeViewModel(factory = viewModelFactory {
        initializer { WifiAnalyzerViewModel(app) }
    })
    val snapshot by viewModel.snapshot.collectAsState()
    val locationState = rememberPermissionState(locationPermissions())

    Scaffold(topBar = { TopAppBar(title = { Text("Wi-Fi Analyzer") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonCyan) {
                    StatusBadge(
                        if (snapshot?.isWifiConnected == true) "Connected to Wi-Fi" else "Not connected to Wi-Fi",
                        if (snapshot?.isWifiConnected == true) StatusLevel.OK else StatusLevel.WARNING,
                    )
                }
            }
            if (!locationState.granted) {
                item {
                    NeonCard(accentColor = NeonPurple) {
                        Text("SSID/BSSID require location permission", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Android requires location access to read the Wi-Fi network name and access point address, even though this app doesn't use GPS location. We only use it for this.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = locationState.request) { Text("Grant location permission") }
                    }
                }
            }
            item {
                NeonCard(accentColor = NeonGreen) {
                    Text("Network Identity", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    LabelValueRow("SSID", snapshot?.ssid ?: snapshot?.ssidUnavailableReason ?: "—")
                    LabelValueRow("BSSID", snapshot?.bssid ?: "Unavailable")
                }
            }
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Addressing", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    LabelValueRow("Local IPv4", snapshot?.localIpv4 ?: "Unavailable")
                    LabelValueRow("Local IPv6", snapshot?.localIpv6List?.joinToString() ?: "None")
                    LabelValueRow("Gateway", snapshot?.gatewayIpv4 ?: "Unavailable")
                    LabelValueRow("Subnet / CIDR", snapshot?.subnetCidr ?: "Unavailable")
                    LabelValueRow("DNS servers", snapshot?.dnsServers?.joinToString() ?: "Unavailable")
                }
            }
            item {
                NeonCard(accentColor = NeonPurple) {
                    Text("Radio", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    LabelValueRow("Link speed", snapshot?.linkSpeedMbps?.let { "$it Mbps" } ?: "Unavailable")
                    LabelValueRow("Signal", snapshot?.signalStrengthDbm?.let { "$it dBm" } ?: "Unavailable")
                    LabelValueRow("Signal level", snapshot?.signalLevelOutOf?.let { "${it.first}/${it.second}" } ?: "Unavailable")
                    LabelValueRow("Frequency", snapshot?.frequencyMhz?.let { "$it MHz" } ?: "Unavailable")
                }
            }
            item {
                snapshot?.let { LabelValueRow("Last updated", formatTimestamp(it.capturedAtMillis)) }
            }
        }
    }
}
