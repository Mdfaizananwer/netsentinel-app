package com.netsentinel.app.data.scanner

import com.netsentinel.app.data.network.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Discovers responsive hosts within a *bounded, local-subnet-only* IP range.
 *
 * Safety properties (see NetworkUtils.validateBoundedRange, called by the caller before this
 * class is used):
 *  - The range must be fully contained in the device's own local subnet.
 *  - The range is capped at a fixed host count.
 *  - Concurrency is bounded by a semaphore; every probe carries its own timeout.
 *  - Discovery is best-effort: client isolation, host firewalls, VPNs and Android's own
 *    background network restrictions can and will hide real devices. This is surfaced in the
 *    UI copy, not silently hidden here.
 */
class NetworkScanner(
    private val maxConcurrency: Int = 32,
    private val connectTimeoutMillis: Int = 400,
) {
    // A conservative list of commonly-open ports used only to confirm host liveness and to
    // report which of these specific ports responded — never a full 1-65535 port sweep.
    private val probePorts = intArrayOf(80, 443, 22, 445, 8080, 62078 /* common on mobile devices */)

    /**
     * Emits [ScanEvent]s as hosts are discovered. Cancelling the collecting coroutine (e.g. the
     * ViewModel's viewModelScope) cancels all in-flight probes promptly because every probe is a
     * child coroutine that participates in structured concurrency.
     */
    fun scan(range: LongRange): kotlinx.coroutines.flow.Flow<ScanEvent> = channelFlow {
        val total = (range.last - range.first + 1).toInt()
        val startTime = System.currentTimeMillis()
        val semaphore = Semaphore(maxConcurrency)
        val scannedCount = AtomicInteger(0)
        val foundCount = AtomicInteger(0)

        val arpTable = readArpTable()

        try {
            val jobs = range.map { ipLong ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        currentCoroutineContext().ensureActive()
                        val ip = NetworkUtils.longToIp(ipLong)
                        val result = probeHost(ip, arpTable)
                        val scanned = scannedCount.incrementAndGet()
                        send(ScanEvent.Progress(scanned, total, ip))
                        if (result != null) {
                            foundCount.incrementAndGet()
                            send(ScanEvent.HostFound(result))
                        }
                    }
                }
            }
            jobs.awaitAll()
            send(ScanEvent.Completed(foundCount.get(), System.currentTimeMillis() - startTime))
        } catch (ce: CancellationException) {
            // Channel may already be in the process of closing due to cancellation; best-effort notify.
            runCatching { send(ScanEvent.Cancelled(scannedCount.get(), total)) }
            throw ce
        } catch (e: Exception) {
            send(ScanEvent.Failed(e.message ?: "Unknown scan error"))
        }
        // channelFlow closes the channel automatically when this block returns — no manual close()
        // or awaitClose() needed, since every emission originates from inside this builder.
    }

    private suspend fun probeHost(ip: String, arpTable: Map<String, String>): HostResult? {
        val openPorts = mutableListOf<Int>()
        var responseTime = -1L
        var reachableViaIcmp = false

        val elapsed = measureTimeMillis {
            // 1) Lightweight ICMP-style reachability check (InetAddress#isReachable falls back to
            //    a TCP echo on port 7 if raw ICMP sockets are unavailable, which is normal and
            //    expected without root on Android).
            reachableViaIcmp = runCatching {
                withContext(Dispatchers.IO) {
                    InetAddress.getByName(ip).isReachable(connectTimeoutMillis)
                }
            }.getOrDefault(false)

            // 2) Bounded TCP connect probes against a small, fixed port list.
            for (port in probePorts) {
                val open = runCatching {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), connectTimeoutMillis)
                        true
                    }
                }.getOrDefault(false)
                if (open) openPorts.add(port)
            }
        }
        responseTime = elapsed

        val knownMac = arpTable[ip]
        val alive = reachableViaIcmp || openPorts.isNotEmpty() || knownMac != null
        if (!alive) return null

        val hostname = runCatching {
            val addr = InetAddress.getByName(ip)
            // canonicalHostName returns the IP itself when reverse DNS fails; treat that as "unresolved".
            addr.canonicalHostName.takeIf { it != ip }
        }.getOrNull()

        val method = when {
            knownMac != null && !reachableViaIcmp && openPorts.isEmpty() -> DiscoveryMethod.ARP_TABLE
            openPorts.isNotEmpty() -> DiscoveryMethod.TCP_CONNECT
            else -> DiscoveryMethod.ICMP_REACHABLE
        }

        return HostResult(
            ipAddress = ip,
            hostname = hostname,
            macAddress = knownMac,
            discoveryMethod = method,
            responseTimeMillis = responseTime,
            lastSeenMillis = System.currentTimeMillis(),
            openPorts = openPorts,
        )
    }

    /**
     * Reads /proc/net/arp, which Android exposes to apps without special permission for entries
     * the kernel already knows about (typically only hosts this device has already talked to).
     * This is NOT active ARP probing/spoofing — purely reading the existing kernel neighbour
     * table — and it may be empty or restricted on some OEM builds/Android versions.
     */
    private fun readArpTable(): Map<String, String> {
        return runCatching {
            val file = File("/proc/net/arp")
            if (!file.exists()) return emptyMap()
            file.readLines().drop(1).mapNotNull { line ->
                val cols = line.trim().split(Regex("\\s+"))
                if (cols.size >= 4) {
                    val ip = cols[0]
                    val mac = cols[3]
                    if (mac != "00:00:00:00:00:00") ip to mac else null
                } else null
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}
