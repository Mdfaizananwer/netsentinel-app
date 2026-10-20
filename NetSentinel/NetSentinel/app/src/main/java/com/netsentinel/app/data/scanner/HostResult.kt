package com.netsentinel.app.data.scanner

enum class DiscoveryMethod { TCP_CONNECT, ICMP_REACHABLE, ARP_TABLE }

data class HostResult(
    val ipAddress: String,
    val hostname: String?,
    val macAddress: String?,
    val discoveryMethod: DiscoveryMethod,
    val responseTimeMillis: Long,
    val lastSeenMillis: Long,
    val openPorts: List<Int> = emptyList(),
)

sealed class ScanEvent {
    data class Progress(val scanned: Int, val total: Int, val currentIp: String) : ScanEvent()
    data class HostFound(val host: HostResult) : ScanEvent()
    data class Completed(val totalFound: Int, val durationMillis: Long) : ScanEvent()
    data class Cancelled(val scanned: Int, val total: Int) : ScanEvent()
    data class Failed(val reason: String) : ScanEvent()
}
