package com.netsentinel.app.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.control.AllowlistedCommand
import com.netsentinel.app.control.CommandResult
import com.netsentinel.app.control.CompanionAgentManager
import com.netsentinel.app.control.LoopbackCompanionAgentTransport
import com.netsentinel.app.control.PairedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceControlViewModel(app: NetSentinelApp) : ViewModel() {
    // The loopback transport is explicit here: this screen demonstrates and exercises the real
    // pairing/allowlist/audit-log state machine without opening any actual network socket.
    // See CompanionAgentProtocol.kt for what a hardened production transport requires.
    private val manager = CompanionAgentManager(LoopbackCompanionAgentTransport(), app.database.auditLogDao())

    val pairedDevices: StateFlow<Map<String, PairedDevice>> = manager.pairedDevices
    val auditLog = app.database.auditLogDao().observeAll()

    private val _lastResult = MutableStateFlow<String?>(null)
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    fun requestPairing(peerId: String, name: String) = viewModelScope.launch { manager.requestPairing(peerId, name) }
    fun approve(peerId: String) = viewModelScope.launch { manager.approvePairing(peerId) }
    fun revoke(peerId: String) = viewModelScope.launch { manager.revokePairing(peerId) }

    fun sendCommand(peerId: String, command: AllowlistedCommand) = viewModelScope.launch {
        _lastResult.value = when (val r = manager.sendCommand(peerId, command)) {
            is CommandResult.Success -> "OK: ${r.payload}"
            is CommandResult.Rejected -> "Rejected: ${r.reason}"
            is CommandResult.Error -> "Error: ${r.message}"
        }
    }
}
