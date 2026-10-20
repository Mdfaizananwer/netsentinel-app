package com.netsentinel.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.netsentinel.app.capture.CaptureStats
import com.netsentinel.app.capture.CaptureUiState
import com.netsentinel.app.capture.PacketCaptureVpnService
import com.netsentinel.app.capture.Protocol
import com.netsentinel.app.data.db.CaptureSessionDao
import com.netsentinel.app.data.db.CaptureSessionEntity
import com.netsentinel.app.data.db.PacketSampleDao
import com.netsentinel.app.data.db.PacketSampleEntity
import kotlinx.coroutines.flow.StateFlow

class CaptureRepository(
    private val context: Context,
    private val sessionDao: CaptureSessionDao,
    private val sampleDao: PacketSampleDao,
) {
    val state: StateFlow<CaptureUiState> = PacketCaptureVpnService.state

    /** Null means the system VPN consent dialog must be shown first (see prepareIntent()). */
    fun prepareIntent(): Intent? = VpnService.prepare(context)

    fun start() {
        context.startService(Intent(context, PacketCaptureVpnService::class.java))
    }

    fun pause() {
        context.startService(Intent(context, PacketCaptureVpnService::class.java).setAction(PacketCaptureVpnService.ACTION_PAUSE))
    }

    fun resume() {
        context.startService(Intent(context, PacketCaptureVpnService::class.java).setAction(PacketCaptureVpnService.ACTION_RESUME))
    }

    fun stop() {
        context.startService(Intent(context, PacketCaptureVpnService::class.java).setAction(PacketCaptureVpnService.ACTION_STOP))
    }

    /** Persists a finished session plus a bounded sample of its packets (full metadata log, no payload). */
    suspend fun persistSession(startedAt: Long, endedAt: Long, stats: CaptureStats): Long {
        val sessionId = sessionDao.upsert(
            CaptureSessionEntity(
                startedAtMillis = startedAt,
                endedAtMillis = endedAt,
                totalPackets = stats.totalPackets,
                totalBytes = stats.totalBytes,
                tcpCount = stats.tcpCount,
                udpCount = stats.udpCount,
                dnsCount = stats.dnsCount,
                otherCount = stats.otherCount,
            )
        )
        return sessionId
    }

    suspend fun persistPacketBatch(sessionId: Long, packets: List<com.netsentinel.app.capture.ParsedPacket>) {
        if (packets.isEmpty()) return
        sampleDao.insertAll(packets.map {
            PacketSampleEntity(
                sessionId = sessionId,
                timestampMillis = it.timestampMillis,
                sourceIp = it.sourceIp,
                destIp = it.destIp,
                protocol = it.protocol.name,
                sourcePort = it.sourcePort,
                destPort = it.destPort,
                sizeBytes = it.totalLengthBytes,
            )
        })
    }

    fun observeSessions() = sessionDao.observeAll()

    suspend fun samplesForSession(sessionId: Long) = sampleDao.getForSession(sessionId)

    suspend fun deleteSession(sessionId: Long) {
        sampleDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }
}
