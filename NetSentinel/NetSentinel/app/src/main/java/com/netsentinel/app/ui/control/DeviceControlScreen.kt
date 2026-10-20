@file:OptIn(ExperimentalMaterial3Api::class)

package com.netsentinel.app.ui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.control.AllowlistedCommand
import com.netsentinel.app.control.PairingState
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.StatusBadge
import com.netsentinel.app.ui.common.StatusLevel
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

@Composable
fun DeviceControlScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val viewModel: DeviceControlViewModel = composeViewModel(factory = viewModelFactory {
        initializer { DeviceControlViewModel(app) }
    })
    val devices by viewModel.pairedDevices.collectAsState()
    val auditLog by viewModel.auditLog.collectAsState(initial = emptyList())
    val lastResult by viewModel.lastResult.collectAsState()
    var newPeerName by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Device Control") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonPurple) {
                    Text("Being on the same Wi-Fi is not authorization", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    Text(
                        "NetSentinel never controls a device just because it's reachable on the network. Control " +
                            "requires that device to run a matching companion agent, explicitly approve pairing on " +
                            "its own screen, and accept only allowlisted commands over an authenticated, encrypted " +
                            "channel. If a device doesn't support this, it simply can't be controlled from here — " +
                            "and this build demonstrates the pairing/allowlist/audit-log workflow using a local, " +
                            "simulated peer (clearly labeled) rather than a real network transport; see the code " +
                            "comments in CompanionAgentProtocol.kt for what a production transport needs.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Pair a (simulated) device", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    OutlinedTextField(value = newPeerName, onValueChange = { newPeerName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (newPeerName.isNotBlank()) {
                            viewModel.requestPairing("sim-${newPeerName.trim()}", newPeerName.trim())
                            newPeerName = ""
                        }
                    }) { Text("Request pairing") }
                }
            }
            items(devices.values.toList()) { device ->
                NeonCard(accentColor = NeonGreen) {
                    Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(device.state.name, if (device.state == PairingState.ACTIVE) StatusLevel.OK else StatusLevel.WARNING)
                    device.pairedAtMillis?.let { LabelValueRow("Paired at", formatTimestamp(it)) }
                    device.lastSeenMillis?.let { LabelValueRow("Last command", formatTimestamp(it)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (device.state == PairingState.REQUESTED) {
                            Button(onClick = { viewModel.approve(device.peerId) }) { Text("Approve") }
                        }
                        if (device.state == PairingState.ACTIVE) {
                            AllowlistedCommand.entries.forEach { cmd ->
                                OutlinedButton(onClick = { viewModel.sendCommand(device.peerId, cmd) }) { Text(cmd.name) }
                            }
                            OutlinedButton(onClick = { viewModel.revoke(device.peerId) }) { Text("Revoke") }
                        }
                    }
                }
            }
            lastResult?.let { item { Text("Last result: $it", style = MaterialTheme.typography.bodyMedium) } }
            item { Text("Audit log", style = MaterialTheme.typography.titleMedium) }
            items(auditLog) { entry ->
                NeonCard {
                    LabelValueRow(entry.action, formatTimestamp(entry.timestampMillis))
                    LabelValueRow("Peer", entry.peerId)
                    Text(entry.detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
