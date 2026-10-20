@file:OptIn(ExperimentalMaterial3Api::class)

package com.netsentinel.app.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.theme.ErrorRed
import com.netsentinel.app.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Privacy & Data") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("What NetSentinel stores", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Text(
                        "Everything NetSentinel collects — scanned devices, packet-capture metadata, network-change " +
                            "events, and companion-agent pairing/audit logs — is stored only in this app's private, " +
                            "unencrypted-by-the-OS-but-sandboxed local database on this device (android:allowBackup is " +
                            "disabled and auto backup/device-transfer are excluded for this app's data). Nothing is " +
                            "uploaded to any server by this app, ever, without an explicit action you take (such as " +
                            "using the system share sheet from the Export buttons).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("What NetSentinel never stores", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Text(
                        "Packet payload contents, passwords, session cookies, authentication tokens, or any application " +
                            "data — the Packet Capture Lab's parser structurally only reads header fields (addresses, " +
                            "ports, protocol, size) and, for DNS, the queried hostname.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                NeonCard(accentColor = ErrorRed) {
                    Text("Delete all local data", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                    Text("Permanently deletes the device inventory, capture sessions, network-change log, and audit log from this device.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        scope.launch {
                            app.database.clearAllTables()
                        }
                    }) { Text("Delete everything") }
                }
            }
        }
    }
}
