package com.netsentinel.app.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.netsentinel.app.capture.CaptureUiState
import com.netsentinel.app.capture.ParsedPacket
import com.netsentinel.app.capture.ProtocolFilter
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

@Composable
fun PacketCaptureScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val viewModel: PacketCaptureViewModel = composeViewModel(factory = viewModelFactory {
        initializer { PacketCaptureViewModel(app) }
    })
    val state by viewModel.state.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val packets by viewModel.packets.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) viewModel.start()
    }

    fun requestStart() {
        val consentIntent = viewModel.vpnConsentIntentOrNull()
        if (consentIntent != null) vpnLauncher.launch(consentIntent) else viewModel.start()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Packet Capture Lab") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonPurple) {
                    Text("Scope of this lab", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    Text(
                        "This starts a real on-device VPN interface (Android grants a system permission prompt). " +
                            "It captures only metadata (addresses, ports, protocol, size, and plain DNS query names) " +
                            "for traffic the OS routes through this app's interface — never passwords, cookies, tokens, " +
                            "message contents, or other devices' LAN traffic, and it never decrypts TLS. " +
                            "This build does not relay traffic back out to the internet, so other apps may lose " +
                            "connectivity while capture is active — stop the capture to restore normal networking.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                NeonCard(accentColor = NeonCyan) {
                    when (val s = state) {
                        is CaptureUiState.Idle -> Button(onClick = { requestStart() }) { Text("Start capture") }
                        is CaptureUiState.Running -> {
                            Text("Capturing since ${formatTimestamp(s.startedAtMillis)}", style = MaterialTheme.typography.bodyMedium)
                            LabelValueRow("Packets", s.stats.totalPackets.toString())
                            LabelValueRow("Bytes", s.stats.totalBytes.toString())
                            s.stats.protocolDistribution.forEach { (k, v) -> LabelValueRow(k, v.toString()) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = viewModel::pause) { Text("Pause") }
                                Button(onClick = viewModel::stop) { Text("Stop") }
                            }
                        }
                        is CaptureUiState.Paused -> {
                            Text("Paused", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::resume) { Text("Resume") }
                                OutlinedButton(onClick = viewModel::stop) { Text("Stop") }
                            }
                        }
                        is CaptureUiState.Stopped -> {
                            Text("Stopped. Total packets: ${s.stats.totalPackets}, bytes: ${s.stats.totalBytes}")
                            Button(onClick = { requestStart() }) { Text("Start new capture") }
                        }
                        is CaptureUiState.Error -> Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                        is CaptureUiState.RequestingPermission -> Text("Waiting for VPN permission…")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtocolFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { viewModel.setFilter(f) }, label = { Text(f.name) })
                    }
                }
            }
            items(viewModel.filteredPackets().asReversed()) { packet -> PacketRow(packet) }
        }
    }
}

@Composable
private fun PacketRow(packet: ParsedPacket) {
    NeonCard(accentColor = NeonGreen) {
        Text("${packet.protocol} ${packet.sourceIp}:${packet.sourcePort ?: "-"} → ${packet.destIp}:${packet.destPort ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        LabelValueRow("Size", "${packet.totalLengthBytes} B")
        LabelValueRow("Time", formatTimestamp(packet.timestampMillis))
        packet.dnsQueryName?.let { LabelValueRow("DNS query", it) }
    }
}
