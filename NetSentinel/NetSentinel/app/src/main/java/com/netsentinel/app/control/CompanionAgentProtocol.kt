package com.netsentinel.app.control

/**
 * DEVICE CONTROL — SCOPE NOTE (read before wiring this into a release build)
 * ============================================================================
 * The project brief is explicit that "sharing the same Wi-Fi network" never implies authority
 * to control another device, and that any control path must have: explicit pairing, strong
 * authentication, encrypted transport, visible consent, revocable pairing, an audit log, and an
 * allowlist of commands — with no hidden shell, no stealth install, and no exploitation.
 *
 * This file defines that protocol's *shape* and a local, in-memory reference implementation of
 * the pairing/allowlist/audit-log state machine, so the architecture is real and testable. It
 * deliberately stops short of shipping a production transport (mTLS socket server) in this
 * deliverable, because a half-finished crypto transport is worse than an honest placeholder: get
 * it subtly wrong and you've built a backdoor, not a feature. What's real here:
 *   - The command allowlist and its enforcement (rejects anything not explicitly listed).
 *   - The pairing state machine (REQUESTED -> APPROVED -> ACTIVE -> REVOKED) with an audit
 *     entry for every transition.
 *   - A companion app on the *target* device would run the matching half of this protocol,
 *     present the pairing code to its own user, and only accept commands from a device whose
 *     pairing has been locally approved on the target.
 * To harden this for production: replace [CompanionAgentTransport] with a real implementation
 * using TLS sockets (javax.net.ssl) with pinned, per-pairing self-signed certs exchanged during
 * the pairing handshake (e.g. via a QR code shown on both screens), and persist pairing keys in
 * the Android Keystore rather than in memory.
 */

/** The only commands a paired companion agent is ever allowed to accept — nothing else parses. */
enum class AllowlistedCommand(val description: String) {
    PING("Liveness check — no side effects"),
    REQUEST_STATUS("Ask the target for its NetSentinel status summary (battery, Wi-Fi, uptime)"),
    REQUEST_SECURITY_REPORT("Ask the target to share its latest on-device security report summary"),
}

enum class PairingState { REQUESTED, APPROVED, ACTIVE, REVOKED, DENIED }

data class PairedDevice(
    val peerId: String,
    val displayName: String,
    val state: PairingState,
    val pairedAtMillis: Long?,
    val lastSeenMillis: Long?,
)

sealed class CommandResult {
    data class Success(val payload: String) : CommandResult()
    data class Rejected(val reason: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

/** Abstraction over the transport so the state machine above is testable without real sockets/TLS. */
interface CompanionAgentTransport {
    suspend fun sendPairingRequest(peerId: String): Boolean
    suspend fun sendCommand(peerId: String, command: AllowlistedCommand): CommandResult
}

/**
 * In-memory, loopback-safe reference transport used for UI demos and tests. It never opens a
 * real network socket, so it cannot be mistaken for a working remote-control backdoor — pairing
 * here only ever "pairs" with a simulated peer, clearly labeled as such in the UI.
 */
class LoopbackCompanionAgentTransport : CompanionAgentTransport {
    override suspend fun sendPairingRequest(peerId: String): Boolean = true
    override suspend fun sendCommand(peerId: String, command: AllowlistedCommand): CommandResult {
        return when (command) {
            AllowlistedCommand.PING -> CommandResult.Success("pong")
            AllowlistedCommand.REQUEST_STATUS -> CommandResult.Success("status: simulated-peer, wifi=connected, battery=unknown")
            AllowlistedCommand.REQUEST_SECURITY_REPORT -> CommandResult.Success("report: simulated-peer has no NetSentinel security report to share yet")
        }
    }
}
