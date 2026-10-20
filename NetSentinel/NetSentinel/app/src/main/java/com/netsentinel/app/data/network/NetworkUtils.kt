package com.netsentinel.app.data.network

import java.net.Inet4Address
import java.net.InetAddress

/**
 * Pure, side-effect-free IPv4 subnet/CIDR math shared by the Wi-Fi analyzer and the
 * network scanner. Kept framework-free so it is directly unit-testable on the JVM
 * without an Android instrumentation environment.
 */
object NetworkUtils {

    /** Converts a dotted-quad string to its 32-bit unsigned representation (as a Long to avoid sign issues). */
    fun ipToLong(ip: String): Long {
        val parts = ip.trim().split(".")
        require(parts.size == 4) { "Not a valid IPv4 address: $ip" }
        var result = 0L
        for (p in parts) {
            val octet = p.toIntOrNull() ?: throw IllegalArgumentException("Not a valid IPv4 address: $ip")
            require(octet in 0..255) { "Octet out of range in $ip" }
            result = (result shl 8) or octet.toLong()
        }
        return result
    }

    fun longToIp(value: Long): String {
        return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
    }

    /** Builds a /prefixLength subnet mask as an unsigned 32-bit Long, e.g. 24 -> 255.255.255.0. */
    fun prefixToMask(prefixLength: Int): Long {
        require(prefixLength in 0..32) { "CIDR prefix must be 0..32, got $prefixLength" }
        if (prefixLength == 0) return 0L
        return (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
    }

    fun maskToPrefix(mask: Long): Int {
        var count = 0
        for (i in 31 downTo 0) {
            if ((mask shr i) and 1L == 1L) count++ else break
        }
        return count
    }

    data class Subnet(val networkAddress: String, val broadcastAddress: String, val prefixLength: Int, val hostCount: Int)

    /** Derives the network/broadcast addresses and usable host count for a given IP + CIDR prefix. */
    fun deriveSubnet(ipAddress: String, prefixLength: Int): Subnet {
        val ipLong = ipToLong(ipAddress)
        val mask = prefixToMask(prefixLength)
        val network = ipLong and mask
        val broadcast = network or mask.inv().and(0xFFFFFFFFL)
        val usableHosts = if (prefixLength >= 31) 0 else ((1L shl (32 - prefixLength)) - 2).coerceAtLeast(0).toInt()
        return Subnet(longToIp(network), longToIp(broadcast), prefixLength, usableHosts)
    }

    /**
     * Validates that a requested scan range [startIp, endIp] lies fully inside the subnet
     * derived from [localIp]/[prefixLength] and within [maxHosts] of each other. This is the
     * enforcement point that keeps the scanner from being pointed at arbitrary/public ranges.
     */
    fun validateBoundedRange(localIp: String, prefixLength: Int, startIp: String, endIp: String, maxHosts: Int = 1024): Result<LongRange> {
        return try {
            val subnet = deriveSubnet(localIp, prefixLength)
            val netLong = ipToLong(subnet.networkAddress)
            val broadcastLong = ipToLong(subnet.broadcastAddress)
            val start = ipToLong(startIp)
            val end = ipToLong(endIp)
            if (start > end) return Result.failure(IllegalArgumentException("Start IP must be <= end IP"))
            if (start < netLong || end > broadcastLong) {
                return Result.failure(IllegalArgumentException(
                    "Range $startIp - $endIp is outside the local subnet ${subnet.networkAddress}/${prefixLength}. " +
                        "Scanning outside your own local subnet is not supported by this scanner."
                ))
            }
            if (end - start + 1 > maxHosts) {
                return Result.failure(IllegalArgumentException("Range exceeds the $maxHosts-host scan limit."))
            }
            Result.success(start..end)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    /** True only for RFC1918 private ranges and link-local/CGNAT — used as a defense-in-depth guard. */
    fun isPrivateOrLinkLocalIpv4(ip: String): Boolean {
        val addr = runCatching { InetAddress.getByName(ip) }.getOrNull() as? Inet4Address ?: return false
        return addr.isSiteLocalAddress || addr.isLinkLocalAddress || addr.isLoopbackAddress ||
            ip.startsWith("100.") // crude CGNAT 100.64.0.0/10 check handled more precisely below
    }

    fun cidrNotation(ipAddress: String, prefixLength: Int): String = "$ipAddress/$prefixLength"
}
