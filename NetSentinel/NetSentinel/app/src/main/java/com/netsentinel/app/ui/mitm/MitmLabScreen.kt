@file:OptIn(ExperimentalMaterial3Api::class)

package com.netsentinel.app.ui.mitm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.netsentinel.app.data.db.NetworkEventEntity
import com.netsentinel.app.mitm.MitmSimulator
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.StatusBadge
import com.netsentinel.app.ui.common.StatusLevel
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.theme.ErrorRed
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.NeonPurple

@Composable
fun MitmLabScreen(app: NetSentinelApp, onBack: () -> Unit) {
    val viewModel: MitmLabViewModel = composeViewModel(factory = viewModelFactory {
        initializer { MitmLabViewModel(app) }
    })
    val events by viewModel.events.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("MITM Security Lab") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NeonCard(accentColor = NeonPurple) {
                    Text("This is an education-only simulation", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    Text(
                        "The scenarios below use a fictional, in-memory topology and canned sample packets. " +
                            "NetSentinel never performs ARP spoofing, DNS spoofing, traffic redirection, or credential " +
                            "interception against real devices — this screen only explains how those attacks work and " +
                            "how to defend against them.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                NeonCard(accentColor = NeonCyan) {
                    Text("Defensive Monitor (real)", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Text(
                        "This part is real: it periodically compares your gateway's IP/MAC and DNS servers against the " +
                            "previous reading, using the same OS-exposed info the Network Scanner uses. A change is a " +
                            "signal to investigate, not proof of an attack (routers get replaced, DHCP leases change, etc.).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = viewModel::checkNow) { Text("Check now") }
                    if (events.isEmpty()) {
                        StatusBadge("No changes observed yet", StatusLevel.OK)
                    } else {
                        Button(onClick = viewModel::clearEvents) { Text("Clear log") }
                    }
                }
            }
            items(events) { event -> EventRow(event) }
            item {
                NeonCard(accentColor = NeonGreen) {
                    Text("ARP spoofing — simulated walkthrough", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    MitmSimulator.arpSpoofScenario().forEach { step ->
                        Text("${step.step}. ${step.from.name} → ${step.to.name}: ${step.summary}", style = MaterialTheme.typography.bodyMedium)
                        Text(step.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                NeonCard(accentColor = NeonGreen) {
                    Text("DNS spoofing — simulated walkthrough", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    MitmSimulator.dnsSpoofScenario().forEach { step ->
                        Text("${step.step}. ${step.from.name} → ${step.to.name}: ${step.summary}", style = MaterialTheme.typography.bodyMedium)
                        Text(step.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(MitmSimulator.defenseTips) { tip ->
                NeonCard(accentColor = NeonCyan) {
                    Text(tip.title, style = MaterialTheme.typography.titleMedium)
                    Text(tip.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: NetworkEventEntity) {
    NeonCard(accentColor = if (event.severity == "WARNING") ErrorRed else NeonCyan) {
        StatusBadge(event.eventType.replace("_", " "), if (event.severity == "WARNING") StatusLevel.WARNING else StatusLevel.OK)
        LabelValueRow("Previous", event.previousValue ?: "—")
        LabelValueRow("New", event.newValue ?: "—")
        LabelValueRow("Observed", formatTimestamp(event.timestampMillis))
    }
}
