package com.netsentinel.app.capture

/**
 * Parsed metadata for a single IPv4 packet. Deliberately excludes payload bytes — this model
 * cannot represent passwords, cookies, auth tokens, or message contents, by construction.
 */
data class ParsedPacket(
    val timestampMillis: Long,
    val sourceIp: String,
    val destIp: String,
    val protocol: Protocol,
    val sourcePort: Int?,
    val destPort: Int?,
    val totalLengthBytes: Int,
    val dnsQueryName: String?, // only the queried hostname, when this packet is a DNS query (UDP/53)
)

enum class Protocol { TCP, UDP, ICMP, OTHER }

data class CaptureStats(
    val totalPackets: Long = 0,
    val totalBytes: Long = 0,
    val tcpCount: Long = 0,
    val udpCount: Long = 0,
    val dnsCount: Long = 0,
    val otherCount: Long = 0,
) {
    val protocolDistribution: Map<String, Long>
        get() = mapOf("TCP" to tcpCount, "UDP" to udpCount, "DNS" to dnsCount, "Other" to otherCount)
}

enum class ProtocolFilter { ALL, TCP, UDP, DNS, OTHER }

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object RequestingPermission : CaptureUiState()
    data class Running(val stats: CaptureStats, val startedAtMillis: Long) : CaptureUiState()
    object Paused : CaptureUiState()
    data class Stopped(val stats: CaptureStats, val sessionId: Long?) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}
