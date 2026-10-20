package com.netsentinel.app.export

import com.google.common.truth.Truth.assertThat
import com.netsentinel.app.data.db.DeviceEntity
import org.junit.Test

class ExportersTest {

    private val sample = listOf(
        DeviceEntity(
            ipAddress = "192.168.1.10", hostname = "phone.local", macAddress = "AA:BB:CC:DD:EE:FF",
            nickname = "My Phone", discoveryMethod = "TCP_CONNECT", firstSeenMillis = 1000L,
            lastSeenMillis = 2000L, lastResponseTimeMillis = 12L, openPortsCsv = "80,443", isOnline = true,
        ),
        DeviceEntity(
            ipAddress = "192.168.1.20", hostname = null, macAddress = null, nickname = null,
            discoveryMethod = "ICMP_REACHABLE", firstSeenMillis = 1500L, lastSeenMillis = 1500L,
            lastResponseTimeMillis = 40L, openPortsCsv = "", isOnline = false,
        ),
    )

    @Test
    fun `csv export includes header and one row per device`() {
        val csv = CsvExporter.export(sample)
        val lines = csv.trim().lines()
        assertThat(lines).hasSize(3) // header + 2 rows
        assertThat(lines[0]).contains("ip_address")
        assertThat(lines[1]).contains("192.168.1.10")
        assertThat(lines[1]).contains("My Phone")
        assertThat(lines[2]).contains("192.168.1.20")
    }

    @Test
    fun `csv export escapes fields containing commas`() {
        val withComma = sample[0].copy(nickname = "Kitchen, Upstairs")
        val csv = CsvExporter.export(listOf(withComma))
        assertThat(csv).contains("\"Kitchen, Upstairs\"")
    }

    @Test
    fun `json export is valid and contains both devices`() {
        val json = JsonExporter.export(sample)
        assertThat(json).contains("\"ip_address\": \"192.168.1.10\"")
        assertThat(json).contains("\"ip_address\": \"192.168.1.20\"")
        assertThat(json).contains("\"hostname\": null")
        assertThat(json.count { it == '{' }).isEqualTo(json.count { it == '}' })
    }

    @Test
    fun `json export escapes quotes and backslashes`() {
        val tricky = sample[0].copy(nickname = "Say \"hi\" \\ bye")
        val json = JsonExporter.export(listOf(tricky))
        assertThat(json).contains("Say \\\"hi\\\" \\\\ bye")
    }

    @Test
    fun `empty list produces header-only csv and empty json array`() {
        assertThat(CsvExporter.export(emptyList()).trim().lines()).hasSize(1)
        assertThat(JsonExporter.export(emptyList()).trim()).isEqualTo("[\n\n]")
    }
}
