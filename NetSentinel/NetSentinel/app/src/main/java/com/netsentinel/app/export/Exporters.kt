package com.netsentinel.app.export

import com.netsentinel.app.data.db.DeviceEntity

/**
 * CSV/JSON export for the device inventory. Pure string-building, framework-free, so it's
 * directly unit-testable. Fields are escaped per RFC 4180 for CSV.
 */
object CsvExporter {
    private val HEADERS = listOf(
        "ip_address", "hostname", "mac_address", "nickname", "discovery_method",
        "first_seen_epoch_ms", "last_seen_epoch_ms", "last_response_time_ms", "open_ports", "is_online"
    )

    fun export(devices: List<DeviceEntity>): String {
        val sb = StringBuilder()
        sb.append(HEADERS.joinToString(",")).append("\n")
        for (d in devices) {
            val row = listOf(
                d.ipAddress, d.hostname.orEmpty(), d.macAddress.orEmpty(), d.nickname.orEmpty(),
                d.discoveryMethod, d.firstSeenMillis.toString(), d.lastSeenMillis.toString(),
                d.lastResponseTimeMillis.toString(), d.openPortsCsv, d.isOnline.toString()
            )
            sb.append(row.joinToString(",") { escape(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun escape(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field
    }
}

/**
 * Hand-rolled JSON serialization (no org.json / no Gson dependency) so this stays trivially
 * unit-testable under plain JUnit on the JVM without an Android/Robolectric runtime.
 */
object JsonExporter {
    fun export(devices: List<DeviceEntity>): String {
        val items = devices.joinToString(",\n") { d ->
            val ports = d.openPortsCsv.split(",").filter { it.isNotBlank() }.joinToString(", ")
            """
            |  {
            |    "ip_address": ${str(d.ipAddress)},
            |    "hostname": ${strOrNull(d.hostname)},
            |    "mac_address": ${strOrNull(d.macAddress)},
            |    "nickname": ${strOrNull(d.nickname)},
            |    "discovery_method": ${str(d.discoveryMethod)},
            |    "first_seen_epoch_ms": ${d.firstSeenMillis},
            |    "last_seen_epoch_ms": ${d.lastSeenMillis},
            |    "last_response_time_ms": ${d.lastResponseTimeMillis},
            |    "open_ports": [$ports],
            |    "is_online": ${d.isOnline}
            |  }
            """.trimMargin()
        }
        return "[\n$items\n]"
    }

    private fun str(value: String): String = "\"" + escape(value) + "\""
    private fun strOrNull(value: String?): String = if (value == null) "null" else str(value)
    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
}
