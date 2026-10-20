package com.netsentinel.app.capture

/**
 * Parses raw IPv4 packet bytes read from the VpnService TUN interface into [ParsedPacket].
 *
 * What this reads: IPv4 header (version, IHL, total length, protocol, source/dest address),
 * and for TCP/UDP the source/dest ports from the transport header. For UDP/port-53 packets it
 * additionally reads just the queried hostname out of the DNS question section.
 *
 * What this never reads: any byte beyond the transport header for TCP/UDP payload (i.e. no HTTP
 * bodies, no TLS records, no application data of any kind), and it performs no TLS interception,
 * no certificate substitution, and no decryption. IPv6 packets are reported as OTHER/unparsed for
 * this lab version.
 *
 * Framework-free (pure byte-array math) so it's directly unit-testable.
 */
object PacketParser {

    fun parse(buffer: ByteArray, length: Int, timestampMillis: Long): ParsedPacket? {
        if (length < 20) return null // shorter than a minimal IPv4 header
        val versionAndIhl = buffer[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null // IPv6/other not parsed in this lab version
        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < 20 || length < ihl) return null

        val totalLength = ((buffer[2].toInt() and 0xFF) shl 8) or (buffer[3].toInt() and 0xFF)
        val protocolByte = buffer[9].toInt() and 0xFF
        val sourceIp = ipv4ToString(buffer, 12)
        val destIp = ipv4ToString(buffer, 16)

        var sourcePort: Int? = null
        var destPort: Int? = null
        var dnsQuery: String? = null
        val protocol = when (protocolByte) {
            6 -> Protocol.TCP
            17 -> Protocol.UDP
            1 -> Protocol.ICMP
            else -> Protocol.OTHER
        }

        if ((protocol == Protocol.TCP || protocol == Protocol.UDP) && length >= ihl + 4) {
            sourcePort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
            destPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)

            if (protocol == Protocol.UDP && destPort == 53 && length >= ihl + 8) {
                val udpHeaderLen = 8
                val dnsStart = ihl + udpHeaderLen
                dnsQuery = runCatching { parseDnsQueryName(buffer, dnsStart, length) }.getOrNull()
            }
        }

        return ParsedPacket(
            timestampMillis = timestampMillis,
            sourceIp = sourceIp,
            destIp = destIp,
            protocol = protocol,
            sourcePort = sourcePort,
            destPort = destPort,
            totalLengthBytes = if (totalLength in 1..length) totalLength else length,
            dnsQueryName = dnsQuery,
        )
    }

    private fun ipv4ToString(buffer: ByteArray, offset: Int): String {
        return "${buffer[offset].toInt() and 0xFF}.${buffer[offset + 1].toInt() and 0xFF}." +
            "${buffer[offset + 2].toInt() and 0xFF}.${buffer[offset + 3].toInt() and 0xFF}"
    }

    /** Reads only the QNAME of the first question in a DNS message — nothing else from the payload. */
    private fun parseDnsQueryName(buffer: ByteArray, dnsStart: Int, length: Int): String? {
        // DNS header is 12 bytes; QNAME starts right after.
        var pos = dnsStart + 12
        if (pos >= length) return null
        val labels = mutableListOf<String>()
        var guard = 0
        while (pos < length && guard < 128) {
            val labelLen = buffer[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen and 0xC0 == 0xC0) break // compression pointer; skip rather than follow
            pos += 1
            if (pos + labelLen > length) return null
            labels.add(String(buffer, pos, labelLen, Charsets.US_ASCII))
            pos += labelLen
            guard++
        }
        return if (labels.isEmpty()) null else labels.joinToString(".")
    }
}
