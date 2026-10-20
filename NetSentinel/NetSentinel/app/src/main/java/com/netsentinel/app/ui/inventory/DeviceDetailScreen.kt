package com.netsentinel.app.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.db.DeviceEntity
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.formatTimestampFull
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@Composable
fun DeviceDetailScreen(app: NetSentinelApp, ipAddress: String, onBack: () -> Unit) {
    val devices by app.deviceRepository.observeAll().collectAsState(initial = emptyList())
    val device: DeviceEntity? = devices.firstOrNull { it.ipAddress == ipAddress }
    var nickname by remember(device?.nickname) { mutableStateOf(device?.nickname.orEmpty()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text(ipAddress) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (device == null) {
                Text("Device not found in inventory (it may have been removed).")
                return@Column
            }
            NeonCard(accentColor = NeonCyan) {
                Text("Identity", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                LabelValueRow("IP address", device.ipAddress)
                LabelValueRow("Hostname", device.hostname ?: "Unknown — not resolvable via reverse DNS")
                LabelValueRow("MAC address", device.macAddress ?: "Unavailable (not in kernel neighbour table)")
                LabelValueRow("Discovery method", device.discoveryMethod)
            }
            NeonCard(accentColor = NeonGreen) {
                Text("History", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                LabelValueRow("First seen", formatTimestampFull(device.firstSeenMillis))
                LabelValueRow("Last seen", formatTimestampFull(device.lastSeenMillis))
                LabelValueRow("Status", if (device.isOnline) "Online at last scan" else "Unresponsive at last scan")
                LabelValueRow("Last response time", "${device.lastResponseTimeMillis} ms")
                LabelValueRow("Open ports", device.openPortsCsv.ifBlank { "None responded" })
            }
            NeonCard {
                Text("Nickname", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        app.deviceRepository.setNickname(device.ipAddress, nickname)
                        onBack()
                    }
                }) { Text("Save nickname") }
            }
            Button(onClick = {
                scope.launch { app.deviceRepository.delete(device.ipAddress); onBack() }
            }) { Text("Remove from inventory") }
        }
    }
}
