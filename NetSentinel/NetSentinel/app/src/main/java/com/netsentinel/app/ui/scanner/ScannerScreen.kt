@file:OptIn(ExperimentalMaterial3Api::class)

package com.netsentinel.app.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.netsentinel.app.data.scanner.HostResult
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.theme.ErrorRed
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

@Composable
fun ScannerScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val viewModel: ScannerViewModel = composeViewModel(factory = viewModelFactory {
        initializer { ScannerViewModel(app) }
    })
    val ui by viewModel.ui.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Network Scanner") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Scan range (your local subnet only)", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    LabelValueRow("Your subnet", ui.localIp?.let { "$it/${ui.prefixLength}" } ?: "Unavailable")
                    OutlinedTextField(
                        value = ui.startIp, onValueChange = viewModel::updateStartIp,
                        label = { Text("Start IP") }, modifier = Modifier.fillMaxWidth(), enabled = !ui.isScanning,
                    )
                    OutlinedTextField(
                        value = ui.endIp, onValueChange = viewModel::updateEndIp,
                        label = { Text("End IP") }, modifier = Modifier.fillMaxWidth(), enabled = !ui.isScanning,
                    )
                    Text(
                        "Scans are capped at 1024 hosts and refused outside your detected local subnet. Client isolation, firewalls, VPNs and OS restrictions can hide devices — a clean scan is not guaranteed. Review the live results and adjust the range if needed.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.errorMessage?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!ui.isScanning) {
                            Button(onClick = viewModel::startScan) { Text("Start scan") }
                        } else {
                            OutlinedButton(onClick = viewModel::cancelScan) { Text("Cancel") }
                        }
                    }
                    if (ui.isScanning || ui.total > 0) {
                        val progress = if (ui.total > 0) ui.scanned.toFloat() / ui.total else 0f
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("${ui.scanned}/${ui.total} — probing ${ui.currentIp}", style = MaterialTheme.typography.bodyMedium)
                    }
                    ui.lastScanDurationMillis?.let { Text("Last scan took ${it}ms, found ${ui.results.size} host(s)", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            items(ui.results) { host -> HostCard(host) }
        }
    }
}

@Composable
private fun HostCard(host: HostResult) {
    NeonCard(accentColor = NeonGreen) {
        Text(host.ipAddress, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
        LabelValueRow("Hostname", host.hostname ?: "Unknown")
        LabelValueRow("MAC address", host.macAddress ?: "Unavailable")
        LabelValueRow("Discovery method", host.discoveryMethod.name)
        LabelValueRow("Response time", "${host.responseTimeMillis} ms")
        LabelValueRow("Open ports probed", if (host.openPorts.isEmpty()) "None responded" else host.openPorts.joinToString())
        LabelValueRow("Last seen", com.netsentinel.app.ui.common.formatTimestamp(host.lastSeenMillis))
    }
}
