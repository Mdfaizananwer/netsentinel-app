package com.netsentinel.app.ui.inventory

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.db.DeviceEntity
import com.netsentinel.app.ui.common.LabelValueRow
import com.netsentinel.app.ui.common.NeonCard
import com.netsentinel.app.ui.common.StatusBadge
import com.netsentinel.app.ui.common.StatusLevel
import com.netsentinel.app.ui.common.formatTimestamp
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InventoryScreen(app: NetSentinelApp, onBack: () -> Unit, onOpenDevice: (String) -> Unit) {
    val viewModel: InventoryViewModel = composeViewModel(factory = viewModelFactory {
        initializer { InventoryViewModel(app) }
    })
    val devices by viewModel.devices.collectAsState()
    val query by viewModel.query.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScopeCompat()

    Scaffold(topBar = { TopAppBar(title = { Text("Device Inventory") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = viewModel::setQuery,
                    label = { Text("Search by IP, hostname, MAC or nickname") }, modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val csv = viewModel.exportCsv()
                            shareExport(context, "netsentinel_devices.csv", csv, "text/csv")
                        }
                    }) { Text("Export CSV") }
                    Button(onClick = {
                        scope.launch {
                            val json = viewModel.exportJson()
                            shareExport(context, "netsentinel_devices.json", json, "application/json")
                        }
                    }) { Text("Export JSON") }
                }
            }
            items(devices, key = { it.ipAddress }) { device ->
                DeviceRow(device, onClick = { onOpenDevice(device.ipAddress) })
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceEntity, onClick: () -> Unit) {
    NeonCard(
        accentColor = if (device.isOnline) NeonGreen else NeonCyan,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(device.nickname ?: device.hostname ?: device.ipAddress, style = MaterialTheme.typography.titleMedium)
        LabelValueRow("IP", device.ipAddress)
        LabelValueRow("MAC", device.macAddress ?: "Unavailable")
        StatusBadge(if (device.isOnline) "Online" else "Unresponsive at last scan", if (device.isOnline) StatusLevel.OK else StatusLevel.WARNING)
        LabelValueRow("Last seen", formatTimestamp(device.lastSeenMillis))
    }
}

private fun shareExport(context: android.content.Context, filename: String, content: String, mime: String) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, filename)
    file.writeText(content)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share $filename"))
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
