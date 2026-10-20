package com.netsentinel.app.mitm

import android.content.Context
import com.netsentinel.app.data.db.NetworkEventDao
import com.netsentinel.app.data.db.NetworkEventEntity
import com.netsentinel.app.data.network.WifiInfoProvider
import java.io.File

/**
 * Defensive monitor: periodically compares the device's own observed gateway IP/MAC and DNS
 * servers against the previous reading, using only information the OS already exposes
 * (ConnectivityManager/WifiManager for gateway+DNS, /proc/net/arp for the gateway's MAC — the
 * same file the Network Scanner reads for host MACs). It performs no active probing of other
 * devices and cannot, by itself, prove an attack occurred — only that something changed, which
 * is reported to the user as a signal to investigate, not a verdict.
 */
class ArpMonitor(private val context: Context, private val eventDao: NetworkEventDao) {

    private val wifiInfoProvider = WifiInfoProvider(context)
    private var lastGatewayIp: String? = null
    private var lastGatewayMac: String? = null
    private var lastDnsServers: List<String> = emptyList()

    data class CheckResult(val changed: Boolean, val events: List<NetworkEventEntity>)

    suspend fun checkOnce(): CheckResult {
        val snapshot = wifiInfoProvider.currentSnapshot()
        val events = mutableListOf<NetworkEventEntity>()
        val now = System.currentTimeMillis()

        if (snapshot.gatewayIpv4 != null) {
            if (lastGatewayIp != null && lastGatewayIp != snapshot.gatewayIpv4) {
                events.add(NetworkEventEntity(
                    timestampMillis = now, eventType = "GATEWAY_IP_CHANGED",
                    previousValue = lastGatewayIp, newValue = snapshot.gatewayIpv4, severity = "WARNING"
                ))
            }
            lastGatewayIp = snapshot.gatewayIpv4
        }

        val gatewayMac = snapshot.gatewayIpv4?.let { readArpMac(it) }
        if (gatewayMac != null) {
            if (lastGatewayMac != null && lastGatewayMac != gatewayMac) {
                events.add(NetworkEventEntity(
                    timestampMillis = now, eventType = "GATEWAY_MAC_CHANGED",
                    previousValue = lastGatewayMac, newValue = gatewayMac, severity = "WARNING"
                ))
            }
            lastGatewayMac = gatewayMac
        }

        if (snapshot.dnsServers.isNotEmpty()) {
            if (lastDnsServers.isNotEmpty() && lastDnsServers.toSet() != snapshot.dnsServers.toSet()) {
                events.add(NetworkEventEntity(
                    timestampMillis = now, eventType = "DNS_CHANGED",
                    previousValue = lastDnsServers.joinToString(", "),
                    newValue = snapshot.dnsServers.joinToString(", "), severity = "INFO"
                ))
            }
            lastDnsServers = snapshot.dnsServers
        }

        events.forEach { eventDao.insert(it) }
        return CheckResult(events.isNotEmpty(), events)
    }

    private fun readArpMac(ip: String): String? {
        return runCatching {
            File("/proc/net/arp").readLines().drop(1).firstNotNullOfOrNull { line ->
                val cols = line.trim().split(Regex("\\s+"))
                if (cols.size >= 4 && cols[0] == ip && cols[3] != "00:00:00:00:00:00") cols[3] else null
            }
        }.getOrNull()
    }
}
