package com.netsentinel.app.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptureStatsTest {
    @Test
    fun `protocol distribution reflects all counters`() {
        val stats = CaptureStats(totalPackets = 10, totalBytes = 5000, tcpCount = 4, udpCount = 3, dnsCount = 2, otherCount = 1)
        val dist = stats.protocolDistribution
        assertThat(dist["TCP"]).isEqualTo(4L)
        assertThat(dist["UDP"]).isEqualTo(3L)
        assertThat(dist["DNS"]).isEqualTo(2L)
        assertThat(dist["Other"]).isEqualTo(1L)
        assertThat(dist.values.sum()).isAtMost(stats.totalPackets) // DNS is a subset of UDP, not additive
    }

    @Test
    fun `default stats are all zero`() {
        val stats = CaptureStats()
        assertThat(stats.totalPackets).isEqualTo(0)
        assertThat(stats.protocolDistribution.values.all { it == 0L }).isTrue()
    }
}
