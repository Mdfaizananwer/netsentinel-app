package com.netsentinel.app.data.repository

import com.netsentinel.app.data.db.DeviceDao
import com.netsentinel.app.data.db.DeviceEntity
import com.netsentinel.app.data.scanner.HostResult
import com.netsentinel.app.export.CsvExporter
import com.netsentinel.app.export.JsonExporter
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val dao: DeviceDao) {

    fun observeAll(): Flow<List<DeviceEntity>> = dao.observeAll()

    /** Merges a fresh scan result into the persisted inventory, preserving first-seen time and nickname. */
    suspend fun recordScanResult(result: HostResult) {
        val existing = dao.getByIp(result.ipAddress)
        val entity = DeviceEntity(
            ipAddress = result.ipAddress,
            hostname = result.hostname ?: existing?.hostname,
            macAddress = result.macAddress ?: existing?.macAddress,
            nickname = existing?.nickname,
            discoveryMethod = result.discoveryMethod.name,
            firstSeenMillis = existing?.firstSeenMillis ?: result.lastSeenMillis,
            lastSeenMillis = result.lastSeenMillis,
            lastResponseTimeMillis = result.responseTimeMillis,
            openPortsCsv = result.openPorts.joinToString(","),
            isOnline = true,
        )
        dao.upsert(entity)
    }

    /** Marks any device not seen in the most recent scan pass as unresponsive, without deleting history. */
    suspend fun markOfflineExcept(seenIps: List<String>) {
        if (seenIps.isEmpty()) return
        dao.markOfflineExcept(seenIps)
    }

    suspend fun setNickname(ip: String, nickname: String?) = dao.setNickname(ip, nickname?.trim()?.ifBlank { null })

    suspend fun delete(ip: String) = dao.delete(ip)

    suspend fun exportCsv(devices: List<DeviceEntity>): String = CsvExporter.export(devices)
    suspend fun exportJson(devices: List<DeviceEntity>): String = JsonExporter.export(devices)
}
