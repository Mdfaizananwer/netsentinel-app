package com.netsentinel.app.scanner

import com.google.common.truth.Truth.assertThat
import com.netsentinel.app.data.network.NetworkUtils
import com.netsentinel.app.data.scanner.NetworkScanner
import com.netsentinel.app.data.scanner.ScanEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

/**
 * These tests exercise the scanner's real bounded-concurrency/timeout/cancellation behavior
 * against TEST-NET-1 (RFC 5737, 192.0.2.0/24) — reserved for documentation, guaranteed to never
 * resolve to a real reachable host — so probes reliably time out quickly and deterministically
 * rather than depending on an actual network being present in CI.
 */
class NetworkScannerTest {

    @Test
    fun `scan of small unreachable range completes and reports zero hosts within timeout budget`() = runBlocking {
        val scanner = NetworkScanner(maxConcurrency = 8, connectTimeoutMillis = 150)
        val range = NetworkUtils.ipToLong("192.0.2.1")..NetworkUtils.ipToLong("192.0.2.8")

        val events = withTimeout(10_000) { scanner.scan(range).toList() }

        val completed = events.filterIsInstance<ScanEvent.Completed>().single()
        assertThat(completed.totalFound).isEqualTo(0)
        val progressEvents = events.filterIsInstance<ScanEvent.Progress>()
        assertThat(progressEvents).hasSize(8)
    }

    @Test
    fun `cancelling the collecting scope stops the scan promptly`() = runBlocking {
        val scanner = NetworkScanner(maxConcurrency = 4, connectTimeoutMillis = 2000)
        val range = NetworkUtils.ipToLong("192.0.2.1")..NetworkUtils.ipToLong("192.0.2.50")

        val job = async(Dispatchers.Default) {
            scanner.scan(range).toList()
        }
        delay(100) // let a few probes start
        job.cancel()

        // A well-behaved cancellation should unwind promptly rather than running the full
        // 2-second-per-host timeout for all 50 hosts serially.
        withTimeout(3000) {
            job.join()
        }
        assertThat(job.isCancelled).isTrue()
    }
}
