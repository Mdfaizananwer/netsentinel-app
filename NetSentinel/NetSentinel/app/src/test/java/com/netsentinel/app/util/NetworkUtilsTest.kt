package com.netsentinel.app.util

import com.google.common.truth.Truth.assertThat
import com.netsentinel.app.data.network.NetworkUtils
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `ip to long and back round trips`() {
        val ip = "192.168.1.42"
        assertThat(NetworkUtils.longToIp(NetworkUtils.ipToLong(ip))).isEqualTo(ip)
    }

    @Test
    fun `ipToLong rejects malformed address`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NetworkUtils.ipToLong("300.1.1.1")
        }
    }

    @Test
    fun `prefixToMask computes standard slash24`() {
        assertThat(NetworkUtils.longToIp(NetworkUtils.prefixToMask(24))).isEqualTo("255.255.255.0")
    }

    @Test
    fun `maskToPrefix is inverse of prefixToMask`() {
        for (p in 0..32) {
            assertThat(NetworkUtils.maskToPrefix(NetworkUtils.prefixToMask(p))).isEqualTo(p)
        }
    }

    @Test
    fun `deriveSubnet computes network and broadcast for slash24`() {
        val subnet = NetworkUtils.deriveSubnet("192.168.1.42", 24)
        assertThat(subnet.networkAddress).isEqualTo("192.168.1.0")
        assertThat(subnet.broadcastAddress).isEqualTo("192.168.1.255")
        assertThat(subnet.hostCount).isEqualTo(254)
    }

    @Test
    fun `deriveSubnet computes correctly for slash28`() {
        val subnet = NetworkUtils.deriveSubnet("10.0.0.20", 28)
        assertThat(subnet.networkAddress).isEqualTo("10.0.0.16")
        assertThat(subnet.broadcastAddress).isEqualTo("10.0.0.31")
        assertThat(subnet.hostCount).isEqualTo(14)
    }

    @Test
    fun `validateBoundedRange accepts range within subnet`() {
        val result = NetworkUtils.validateBoundedRange("192.168.1.10", 24, "192.168.1.1", "192.168.1.50")
        assertThat(result.isSuccess).isTrue()
        val range = result.getOrThrow()
        assertThat(range.first).isEqualTo(NetworkUtils.ipToLong("192.168.1.1"))
        assertThat(range.last).isEqualTo(NetworkUtils.ipToLong("192.168.1.50"))
    }

    @Test
    fun `validateBoundedRange rejects range outside local subnet`() {
        val result = NetworkUtils.validateBoundedRange("192.168.1.10", 24, "8.8.8.0", "8.8.8.255")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `validateBoundedRange rejects start greater than end`() {
        val result = NetworkUtils.validateBoundedRange("192.168.1.10", 24, "192.168.1.50", "192.168.1.1")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `validateBoundedRange rejects range exceeding host cap`() {
        val result = NetworkUtils.validateBoundedRange("10.0.0.5", 8, "10.0.0.1", "10.255.255.254", maxHosts = 1024)
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `cidrNotation formats correctly`() {
        assertThat(NetworkUtils.cidrNotation("192.168.1.0", 24)).isEqualTo("192.168.1.0/24")
    }
}
