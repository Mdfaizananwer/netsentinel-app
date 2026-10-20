package com.netsentinel.app.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

class PacketParserTest {

    /** Builds a minimal, valid IPv4 header (no options) + an optional transport header. */
    private fun buildIpv4Packet(
        protocol: Int,
        srcIp: String,
        dstIp: String,
        transport: ByteArray = ByteArray(0),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val totalLength = 20 + transport.size
        out.write(0x45) // version 4, IHL 5 (20 bytes)
        out.write(0x00) // DSCP/ECN
        out.write((totalLength shr 8) and 0xFF); out.write(totalLength and 0xFF)
        out.write(0x00); out.write(0x00) // identification
        out.write(0x00); out.write(0x00) // flags/fragment offset
        out.write(64) // TTL
        out.write(protocol)
        out.write(0x00); out.write(0x00) // header checksum (not validated by our parser)
        srcIp.split(".").forEach { out.write(it.toInt()) }
        dstIp.split(".").forEach { out.write(it.toInt()) }
        out.write(transport)
        return out.toByteArray()
    }

    private fun udpHeader(srcPort: Int, dstPort: Int, payload: ByteArray = ByteArray(0)): ByteArray {
        val out = ByteArrayOutputStream()
        val length = 8 + payload.size
        out.write((srcPort shr 8) and 0xFF); out.write(srcPort and 0xFF)
        out.write((dstPort shr 8) and 0xFF); out.write(dstPort and 0xFF)
        out.write((length shr 8) and 0xFF); out.write(length and 0xFF)
        out.write(0x00); out.write(0x00) // checksum
        out.write(payload)
        return out.toByteArray()
    }

    private fun tcpHeader(srcPort: Int, dstPort: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write((srcPort shr 8) and 0xFF); out.write(srcPort and 0xFF)
        out.write((dstPort shr 8) and 0xFF); out.write(dstPort and 0xFF)
        repeat(16) { out.write(0x00) } // remaining TCP header fields, not needed by the parser
        return out.toByteArray()
    }

    @Test
    fun `parses TCP packet addresses and ports`() {
        val packet = buildIpv4Packet(6, "192.168.1.10", "93.184.216.34", tcpHeader(51000, 443))
        val parsed = PacketParser.parse(packet, packet.size, 1000L)!!
        assertThat(parsed.protocol).isEqualTo(Protocol.TCP)
        assertThat(parsed.sourceIp).isEqualTo("192.168.1.10")
        assertThat(parsed.destIp).isEqualTo("93.184.216.34")
        assertThat(parsed.sourcePort).isEqualTo(51000)
        assertThat(parsed.destPort).isEqualTo(443)
    }

    @Test
    fun `parses UDP packet`() {
        val packet = buildIpv4Packet(17, "192.168.1.10", "8.8.8.8", udpHeader(55000, 53))
        val parsed = PacketParser.parse(packet, packet.size, 1000L)!!
        assertThat(parsed.protocol).isEqualTo(Protocol.UDP)
        assertThat(parsed.destPort).isEqualTo(53)
    }

    @Test
    fun `extracts DNS query name from UDP 53 payload`() {
        val dnsHeader = ByteArray(12) // minimal fixed DNS header, flags irrelevant to parser
        val qname = byteArrayOf(7) + "example".toByteArray() + byteArrayOf(3) + "com".toByteArray() + byteArrayOf(0)
        val dnsPayload = dnsHeader + qname
        val packet = buildIpv4Packet(17, "192.168.1.10", "8.8.8.8", udpHeader(55000, 53, dnsPayload))
        val parsed = PacketParser.parse(packet, packet.size, 1000L)!!
        assertThat(parsed.dnsQueryName).isEqualTo("example.com")
    }

    @Test
    fun `non-dns udp packet has null dns query name`() {
        val packet = buildIpv4Packet(17, "192.168.1.10", "8.8.8.8", udpHeader(55000, 5353))
        val parsed = PacketParser.parse(packet, packet.size, 1000L)!!
        assertThat(parsed.dnsQueryName).isNull()
    }

    @Test
    fun `rejects non-ipv4 packets`() {
        val buffer = ByteArray(20)
        buffer[0] = 0x60 // version 6
        assertThat(PacketParser.parse(buffer, buffer.size, 0L)).isNull()
    }

    @Test
    fun `rejects truncated buffer shorter than minimal header`() {
        val buffer = ByteArray(10)
        assertThat(PacketParser.parse(buffer, buffer.size, 0L)).isNull()
    }

    @Test
    fun `parsed packet never exposes payload bytes beyond transport header`() {
        // Structural guarantee check: ParsedPacket has no field that could hold payload content
        // other than the deliberately-limited DNS query name.
        val fields = ParsedPacket::class.java.declaredFields.map { it.name }
        assertThat(fields).doesNotContain("payload")
        assertThat(fields).doesNotContain("payloadBytes")
    }
}
