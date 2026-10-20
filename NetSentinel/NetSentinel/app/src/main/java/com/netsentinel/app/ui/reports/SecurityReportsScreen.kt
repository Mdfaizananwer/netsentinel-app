package com.netsentinel.app.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.formatTimestampFull
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

/**
 * Aggregates real, locally-collected data into a single "what did NetSentinel find" view:
 * device inventory summary, capture session history, and the MITM defensive-monitor log.
 * This is a composed report over data already gathered elsewhere, not a separate scan engine.
 */
@Composable
fun SecurityReportsScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val devices by app.deviceRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by app.captureRepository.observeSessions().collectAsState(initial = emptyList())
    val events by app.mitmMonitorRepository.observeEvents().collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("Security Reports") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Network summary", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    LabelValueRow("Known devices", devices.size.toString())
                    LabelValueRow("Online at last scan", devices.count { it.isOnline }.toString())
                    LabelValueRow("Devices missing a hostname", devices.count { it.hostname == null }.toString())
                    LabelValueRow("Capture sessions recorded", sessions.size.toString())
                    LabelValueRow("Defensive-monitor events", events.size.toString())
                }
            }
            if (sessions.isNotEmpty()) {
                item { Text("Capture sessions", style = MaterialTheme.typography.titleMedium) }
                items(sessions) { s ->
                    NeonCard(accentColor = NeonGreen) {
                        LabelValueRow("Started", formatTimestampFull(s.startedAtMillis))
                        LabelValueRow("Packets", s.totalPackets.toString())
                        LabelValueRow("Bytes", s.totalBytes.toString())
                        LabelValueRow("TCP / UDP / DNS / Other", "${s.tcpCount} / ${s.udpCount} / ${s.dnsCount} / ${s.otherCount}")
                    }
                }
            }
            if (events.isNotEmpty()) {
                item { Text("Flagged network changes", style = MaterialTheme.typography.titleMedium) }
                items(events.take(10)) { e ->
                    NeonCard(accentColor = NeonPurple) {
                        LabelValueRow(e.eventType, formatTimestampFull(e.timestampMillis))
                        LabelValueRow("Change", "${e.previousValue} → ${e.newValue}")
                    }
                }
            }
        }
    }
}
