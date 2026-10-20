package com.netsentinel.app.control

import com.netsentinel.app.data.db.AuditLogDao
import com.netsentinel.app.data.db.AuditLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pairing + allowlist enforcement state machine described in CompanionAgentProtocol.kt.
 * Every transition writes an audit-log row; nothing is silent.
 */
class CompanionAgentManager(
    private val transport: CompanionAgentTransport,
    private val auditLogDao: AuditLogDao,
) {
    private val _pairedDevices = MutableStateFlow<Map<String, PairedDevice>>(emptyMap())
    val pairedDevices: StateFlow<Map<String, PairedDevice>> = _pairedDevices.asStateFlow()

    suspend fun requestPairing(peerId: String, displayName: String) {
        audit(peerId, "PAIR_REQUEST", "Requested pairing with $displayName")
        val accepted = transport.sendPairingRequest(peerId)
        val state = if (accepted) PairingState.REQUESTED else PairingState.DENIED
        _pairedDevices.update { it + (peerId to PairedDevice(peerId, displayName, state, null, null)) }
    }

    /** Called when the *user* (not the remote peer) approves a pending pairing request — explicit, local consent. */
    suspend fun approvePairing(peerId: String) {
        val existing = _pairedDevices.value[peerId] ?: return
        val now = System.currentTimeMillis()
        _pairedDevices.update { it + (peerId to existing.copy(state = PairingState.ACTIVE, pairedAtMillis = now)) }
        audit(peerId, "PAIR_APPROVED", "User approved pairing with ${existing.displayName}")
    }

    suspend fun revokePairing(peerId: String) {
        val existing = _pairedDevices.value[peerId] ?: return
        _pairedDevices.update { it + (peerId to existing.copy(state = PairingState.REVOKED)) }
        audit(peerId, "PAIR_REVOKED", "User revoked pairing with ${existing.displayName}")
    }

    suspend fun sendCommand(peerId: String, command: AllowlistedCommand): CommandResult {
        val device = _pairedDevices.value[peerId]
        if (device == null || device.state != PairingState.ACTIVE) {
            audit(peerId, "COMMAND_SENT", "Rejected: not an actively-paired device (${command.name})")
            return CommandResult.Rejected("Device is not actively paired")
        }
        audit(peerId, "COMMAND_SENT", command.name)
        val result = transport.sendCommand(peerId, command)
        audit(peerId, "COMMAND_RESULT", result.toString())
        _pairedDevices.update { it + (peerId to device.copy(lastSeenMillis = System.currentTimeMillis())) }
        return result
    }

    private suspend fun audit(peerId: String, action: String, detail: String) {
        auditLogDao.insert(AuditLogEntity(timestampMillis = System.currentTimeMillis(), peerId = peerId, action = action, detail = detail))
    }
}
